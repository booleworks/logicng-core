// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MODEL_ENUMERATION_STARTED;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.generateBlockingClause;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.relevantAllIndicesFromSolver;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.relevantIndicesFromSolver;
import static com.booleworks.logicng.util.CollectionHelper.difference;
import static com.booleworks.logicng.util.CollectionHelper.nullSafe;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.SolverFunction;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A solver function for enumerating models on the solver.
 * @param <RESULT> The result type of the model enumeration function. Can be
 *                 e.g. a model count, a list of models, or a BDD.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class AbstractModelEnumerationFunction<RESULT> implements SolverFunction<RESULT> {

    protected final SortedSet<Variable> variables;
    protected final SortedSet<Variable> additionalVariables;
    protected final ModelEnumerationStrategy strategy;

    protected AbstractModelEnumerationFunction(final SortedSet<Variable> variables,
                                               final SortedSet<Variable> additionalVariables,
                                               final ModelEnumerationConfig configuration) {
        this.variables = variables;
        this.additionalVariables = additionalVariables;
        strategy = configuration.strategy == null ? NoSplitModelEnumerationStrategy.get() : configuration.strategy;
    }

    protected abstract EnumerationCollector<RESULT> newCollector(final FormulaFactory f,
                                                                 final SortedSet<Variable> knownVariables,
                                                                 final SortedSet<Variable> dontCareVariablesNotOnSolver,
                                                                 SortedSet<Variable> additionalVariablesNotOnSolver);

    @Override
    public LngResult<RESULT> apply(final SatSolver solver, final ComputationHandler handler) {
        if (!handler.shouldResume(MODEL_ENUMERATION_STARTED)) {
            return LngResult.canceled(MODEL_ENUMERATION_STARTED);
        }
        final SortedSet<Variable> knownVariables = solver.getUnderlyingSolver().knownVariables();
        final SortedSet<Variable> additionalVarsNotOnSolver =
                difference(additionalVariables, knownVariables, TreeSet::new);
        final SortedSet<Variable> dontCareVariablesNotOnSolver = difference(variables, knownVariables, TreeSet::new);
        final EnumerationCollector<RESULT> collector =
                newCollector(solver.getFactory(), knownVariables, dontCareVariablesNotOnSolver, additionalVarsNotOnSolver);
        final SortedSet<Variable> enumerationVars =
                knownVariables.stream().filter(variables::contains).collect(Collectors.toCollection(TreeSet::new));
        final SortedSet<Variable> initialSplitVars =
                nullSafe(() -> strategy.splitVarsForRecursionDepth(enumerationVars, solver, 0), TreeSet::new);
        final LngEvent cancelCause = enumerateRecursive(collector, solver, new TreeSet<>(), enumerationVars, initialSplitVars, 0, handler);
        final RESULT result = collector.getResult();
        if (cancelCause == null) {
            return LngResult.of(result);
        } else {
            return LngResult.partial(result, cancelCause);
        }
    }

    private LngEvent enumerateRecursive(final EnumerationCollector<RESULT> collector, final SatSolver solver,
                                        final SortedSet<Literal> splitModel, final SortedSet<Variable> enumerationVars,
                                        final SortedSet<Variable> splitVars, final int recursionDepth, final ComputationHandler handler) {
        final int maxNumberOfModelsForEnumeration = strategy.maxNumberOfModelsForEnumeration(recursionDepth);
        final SolverState state = solver.saveState();
        solver.add(splitModel);
        final LngResult<Boolean> enumerationSucceeded = enumerate(collector, solver, enumerationVars,
                additionalVariables, maxNumberOfModelsForEnumeration, handler);
        if (!enumerationSucceeded.isSuccess()) {
            collector.commit(handler);
            return enumerationSucceeded.getCancelCause();
        }
        if (!enumerationSucceeded.getResult()) {
            final LngEvent cancelCause = collector.rollback(handler);
            if (cancelCause != null) {
                solver.loadState(state);
                return cancelCause;
            }
            SortedSet<Variable> newSplitVars = new TreeSet<>(splitVars);
            final int maxNumberOfModelsForSplitAssignments =
                    strategy.maxNumberOfModelsForSplitAssignments(recursionDepth);
            while (true) {
                final LngResult<Boolean> enumerationForSplit = enumerate(
                        collector, solver, newSplitVars, null, maxNumberOfModelsForSplitAssignments, handler);
                if (!enumerationForSplit.isSuccess()) {
                    solver.loadState(state);
                    collector.rollback(handler);
                    return enumerationForSplit.getCancelCause();
                } else if (enumerationForSplit.getResult()) {
                    break;
                } else {
                    final LngEvent cancelationOnRollback = collector.rollback(handler);
                    if (cancelationOnRollback != null) {
                        solver.loadState(state);
                        return cancelationOnRollback;
                    }
                    newSplitVars = strategy.reduceSplitVars(newSplitVars, recursionDepth);
                }
            }

            final SortedSet<Variable> remainingVars = new TreeSet<>(enumerationVars);
            remainingVars.removeAll(newSplitVars);
            for (final Literal literal : splitModel) {
                remainingVars.remove(literal.variable());
            }

            final LngResult<List<Model>> newSplitResult = collector.rollbackAndReturnModels(solver, handler);
            if (!newSplitResult.isSuccess()) {
                solver.loadState(state);
                return newSplitResult.getCancelCause();
            }
            final SortedSet<Variable> recursiveSplitVars =
                    strategy.splitVarsForRecursionDepth(remainingVars, solver, recursionDepth + 1);
            for (final Model newSplitAssignment : newSplitResult.getPartialResult()) {
                final SortedSet<Literal> recursiveSplitModel = new TreeSet<>(newSplitAssignment.getLiterals());
                recursiveSplitModel.addAll(splitModel);
                enumerateRecursive(collector, solver, recursiveSplitModel, enumerationVars, recursiveSplitVars,
                        recursionDepth + 1, handler);
                final LngEvent commitEvent = collector.commit(handler);
                if (commitEvent != null) {
                    solver.loadState(state);
                    return commitEvent;
                }
            }
        } else {
            final LngEvent commitEvent = collector.commit(handler);
            if (commitEvent != null) {
                solver.loadState(state);
                return commitEvent;
            }
        }
        solver.loadState(state);
        return null;
    }

    protected static <R> LngResult<Boolean> enumerate(final EnumerationCollector<R> collector, final SatSolver solver,
                                                      final SortedSet<Variable> variables,
                                                      final SortedSet<Variable> additionalVariables, final int maxModels,
                                                      final ComputationHandler handler) {
        final SolverState stateBeforeEnumeration = solver.saveState();
        final LngIntVector relevantIndices = relevantIndicesFromSolver(variables, solver);
        final LngIntVector relevantAllIndices =
                relevantAllIndicesFromSolver(variables, additionalVariables, relevantIndices, solver);

        int foundModels = 0;
        LngEvent cancelCause = null;
        while (modelEnumerationSatCall(solver, handler)) {
            final LngBooleanVector modelFromSolver = solver.getUnderlyingSolver().model();
            if (++foundModels >= maxModels) {
                solver.loadState(stateBeforeEnumeration);
                return LngResult.of(false);
            }
            cancelCause = collector.addModel(modelFromSolver, solver, relevantAllIndices, handler);
            if (cancelCause == null && !modelFromSolver.isEmpty()) {
                final LngIntVector blockingClause = generateBlockingClause(modelFromSolver, relevantIndices);
                solver.getUnderlyingSolver().addClause(blockingClause, null);
            } else {
                break;
            }
        }
        solver.loadState(stateBeforeEnumeration);
        return cancelCause == null ? LngResult.of(true) : LngResult.canceled(cancelCause);
    }

    private static boolean modelEnumerationSatCall(final SatSolver solver, final ComputationHandler handler) {
        final LngResult<Boolean> sat = solver.satCall().handler(handler).sat();
        return sat.isSuccess() && sat.getResult();
    }

    protected static FormulaFactory factory(final SortedSet<Variable> variables) {
        return variables == null || variables.isEmpty() ? FormulaFactory.caching() : variables.first().getFactory();
    }

    protected static ModelEnumerationConfig configuration(final SortedSet<Variable> variables,
                                                          final ModelEnumerationConfig config) {
        return config == null
                ? (ModelEnumerationConfig) factory(variables).configurationFor(ConfigurationType.MODEL_ENUMERATION)
                : config;
    }

}
