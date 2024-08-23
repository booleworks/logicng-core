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

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.LNGResultWithPartial;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.solvers.SATSolver;
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
    public LNGResultWithPartial<RESULT, RESULT> apply(final SATSolver solver, final ComputationHandler handler) {
        if (!handler.shouldResume(MODEL_ENUMERATION_STARTED)) {
            return LNGResultWithPartial.canceled(null, MODEL_ENUMERATION_STARTED);
        }
        final SortedSet<Variable> knownVariables = solver.underlyingSolver().knownVariables();
        final SortedSet<Variable> additionalVarsNotOnSolver =
                difference(additionalVariables, knownVariables, TreeSet::new);
        final SortedSet<Variable> dontCareVariablesNotOnSolver = difference(variables, knownVariables, TreeSet::new);
        final EnumerationCollector<RESULT> collector =
                newCollector(solver.factory(), knownVariables, dontCareVariablesNotOnSolver, additionalVarsNotOnSolver);
        final SortedSet<Variable> enumerationVars =
                knownVariables.stream().filter(variables::contains).collect(Collectors.toCollection(TreeSet::new));
        final SortedSet<Variable> initialSplitVars =
                nullSafe(() -> strategy.splitVarsForRecursionDepth(enumerationVars, solver, 0), TreeSet::new);
        final LNGEvent cancelCause = enumerateRecursive(collector, solver, new TreeSet<>(), enumerationVars, initialSplitVars, 0, handler);
        final RESULT result = collector.getResult();
        if (cancelCause == null) {
            return LNGResultWithPartial.ofResult(result);
        } else {
            return LNGResultWithPartial.canceled(result, cancelCause);
        }
    }

    private LNGEvent enumerateRecursive(final EnumerationCollector<RESULT> collector, final SATSolver solver,
                                        final SortedSet<Literal> splitModel, final SortedSet<Variable> enumerationVars,
                                        final SortedSet<Variable> splitVars, final int recursionDepth, final ComputationHandler handler) {
        final int maxNumberOfModelsForEnumeration = strategy.maxNumberOfModelsForEnumeration(recursionDepth);
        final SolverState state = solver.saveState();
        solver.add(splitModel);
        final LNGResult<Boolean> enumerationSucceeded = enumerate(collector, solver, enumerationVars,
                additionalVariables, maxNumberOfModelsForEnumeration, handler);
        if (!enumerationSucceeded.isSuccess()) {
            collector.commit(handler);
            return enumerationSucceeded.getCancelCause();
        }
        if (!enumerationSucceeded.getResult()) {
            final LNGEvent cancelCause = collector.rollback(handler);
            if (cancelCause != null) {
                solver.loadState(state);
                return cancelCause;
            }
            SortedSet<Variable> newSplitVars = new TreeSet<>(splitVars);
            final int maxNumberOfModelsForSplitAssignments =
                    strategy.maxNumberOfModelsForSplitAssignments(recursionDepth);
            while (true) {
                final LNGResult<Boolean> enumerationForSplit = enumerate(
                        collector, solver, newSplitVars, null, maxNumberOfModelsForSplitAssignments, handler);
                if (!enumerationForSplit.isSuccess()) {
                    solver.loadState(state);
                    collector.rollback(handler);
                    return enumerationForSplit.getCancelCause();
                } else if (enumerationForSplit.getResult()) {
                    break;
                } else {
                    final LNGEvent cancellationOnRollback = collector.rollback(handler);
                    if (cancellationOnRollback != null) {
                        solver.loadState(state);
                        return cancellationOnRollback;
                    }
                    newSplitVars = strategy.reduceSplitVars(newSplitVars, recursionDepth);
                }
            }

            final SortedSet<Variable> remainingVars = new TreeSet<>(enumerationVars);
            remainingVars.removeAll(newSplitVars);
            for (final Literal literal : splitModel) {
                remainingVars.remove(literal.variable());
            }

            final List<Model> newSplitAssignments = collector.rollbackAndReturnModels(solver, handler);
            final SortedSet<Variable> recursiveSplitVars =
                    strategy.splitVarsForRecursionDepth(remainingVars, solver, recursionDepth + 1);
            for (final Model newSplitAssignment : newSplitAssignments) {
                final SortedSet<Literal> recursiveSplitModel = new TreeSet<>(newSplitAssignment.getLiterals());
                recursiveSplitModel.addAll(splitModel);
                enumerateRecursive(collector, solver, recursiveSplitModel, enumerationVars, recursiveSplitVars,
                        recursionDepth + 1, handler);
                final LNGEvent commitEvent = collector.commit(handler);
                if (commitEvent != null) {
                    solver.loadState(state);
                    return commitEvent;
                }
            }
        } else {
            final LNGEvent commitEvent = collector.commit(handler);
            if (commitEvent != null) {
                solver.loadState(state);
                return commitEvent;
            }
        }
        solver.loadState(state);
        return null;
    }

    protected static <R> LNGResult<Boolean> enumerate(final EnumerationCollector<R> collector, final SATSolver solver,
                                                      final SortedSet<Variable> variables,
                                                      final SortedSet<Variable> additionalVariables, final int maxModels,
                                                      final ComputationHandler handler) {
        final SolverState stateBeforeEnumeration = solver.saveState();
        final LNGIntVector relevantIndices = relevantIndicesFromSolver(variables, solver);
        final LNGIntVector relevantAllIndices =
                relevantAllIndicesFromSolver(variables, additionalVariables, relevantIndices, solver);

        int foundModels = 0;
        LNGEvent cancelCause = null;
        while (modelEnumerationSATCall(solver, handler)) {
            final LNGBooleanVector modelFromSolver = solver.underlyingSolver().model();
            if (++foundModels >= maxModels) {
                solver.loadState(stateBeforeEnumeration);
                return LNGResult.of(false);
            }
            cancelCause = collector.addModel(modelFromSolver, solver, relevantAllIndices, handler);
            if (cancelCause == null && modelFromSolver.size() > 0) {
                final LNGIntVector blockingClause = generateBlockingClause(modelFromSolver, relevantIndices);
                solver.underlyingSolver().addClause(blockingClause, null);
            } else {
                break;
            }
        }
        solver.loadState(stateBeforeEnumeration);
        return cancelCause == null ? LNGResult.of(true) : LNGResult.canceled(cancelCause);
    }

    private static boolean modelEnumerationSATCall(final SATSolver solver, final ComputationHandler handler) {
        final LNGResult<Boolean> sat = solver.satCall().handler(handler).sat();
        return sat.isSuccess() && sat.getResult();
    }

    protected static FormulaFactory factory(final SortedSet<Variable> variables) {
        return variables == null || variables.isEmpty() ? FormulaFactory.caching() : variables.first().factory();
    }

    protected static ModelEnumerationConfig configuration(final SortedSet<Variable> variables,
                                                          final ModelEnumerationConfig config) {
        return config == null
                ? (ModelEnumerationConfig) factory(variables).configurationFor(ConfigurationType.MODEL_ENUMERATION)
                : config;
    }

}
