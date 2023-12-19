// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import static com.booleworks.logicng.datastructures.Tristate.TRUE;
import static com.booleworks.logicng.datastructures.Tristate.UNDEF;
import static com.booleworks.logicng.formulas.FormulaFactory.CC_PREFIX;
import static com.booleworks.logicng.formulas.FormulaFactory.CNF_PREFIX;
import static com.booleworks.logicng.formulas.FormulaFactory.PB_PREFIX;
import static com.booleworks.logicng.handlers.Handler.aborted;
import static com.booleworks.logicng.handlers.Handler.start;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.generateBlockingClause;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.relevantAllIndicesFromSolver;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.relevantIndicesFromSolver;
import static com.booleworks.logicng.util.CollectionHelper.difference;
import static com.booleworks.logicng.util.CollectionHelper.nullSafe;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.SolverFunction;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
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
    protected final ModelEnumerationHandler handler;
    protected final ModelEnumerationStrategy strategy;

    protected AbstractModelEnumerationFunction(final SortedSet<Variable> variables,
                                               final SortedSet<Variable> additionalVariables,
                                               final ModelEnumerationConfig configuration) {
        this.variables = variables;
        this.additionalVariables = additionalVariables;
        handler = configuration.handler;
        strategy = configuration.strategy == null ? NoSplitModelEnumerationStrategy.get() : configuration.strategy;
    }

    protected abstract EnumerationCollector<RESULT> newCollector(final FormulaFactory f,
                                                                 final SortedSet<Variable> knownVariables,
                                                                 final SortedSet<Variable> dontCareVariablesNotOnSolver,
                                                                 SortedSet<Variable> additionalVariablesNotOnSolver);

    @Override
    public RESULT apply(final MiniSat solver, final Consumer<Tristate> resultSetter) {
        if (!(strategy instanceof NoSplitModelEnumerationStrategy) && !solver.canSaveLoadState()) {
            throw new IllegalArgumentException(
                    "Recursive model enumeration function can only be applied to solvers with load/save state capability.");
        }
        start(handler);
        final SortedSet<Variable> knownVariables = solver.knownVariables();
        final SortedSet<Variable> additionalVarsNotOnSolver =
                difference(additionalVariables, knownVariables, TreeSet::new);
        final SortedSet<Variable> dontCareVariablesNotOnSolver = difference(variables, knownVariables, TreeSet::new);
        final EnumerationCollector<RESULT> collector =
                newCollector(solver.factory(), knownVariables, dontCareVariablesNotOnSolver, additionalVarsNotOnSolver);
        final SortedSet<Variable> enumerationVars = getVarsForEnumeration(knownVariables);
        final SortedSet<Variable> initialSplitVars =
                nullSafe(() -> strategy.splitVarsForRecursionDepth(enumerationVars, solver, 0), TreeSet::new);
        enumerateRecursive(collector, solver, new Model(), resultSetter, enumerationVars, initialSplitVars, 0);
        return collector.getResult();
    }

    private void enumerateRecursive(final EnumerationCollector<RESULT> collector, final MiniSat solver,
                                    final Model splitModel, final Consumer<Tristate> resultSetter,
                                    final SortedSet<Variable> enumerationVars, final SortedSet<Variable> splitVars,
                                    final int recursionDepth) {
        final int maxNumberOfModelsForEnumeration = strategy.maxNumberOfModelsForEnumeration(recursionDepth);
        final SolverState state = saveState(solver);
        solver.add(splitModel.formula(solver.factory()));
        final boolean enumerationFinished = enumerate(collector, solver, resultSetter, enumerationVars,
                additionalVariables, maxNumberOfModelsForEnumeration, handler);
        if (!enumerationFinished) {
            if (!collector.rollback(handler)) {
                loadState(solver, state);
                return;
            }
            SortedSet<Variable> newSplitVars = new TreeSet<>(splitVars);
            final int maxNumberOfModelsForSplitAssignments =
                    strategy.maxNumberOfModelsForSplitAssignments(recursionDepth);
            while (!enumerate(collector, solver, resultSetter, newSplitVars, null, maxNumberOfModelsForSplitAssignments,
                    handler)) {
                if (!collector.rollback(handler)) {
                    loadState(solver, state);
                    return;
                }
                newSplitVars = strategy.reduceSplitVars(newSplitVars, recursionDepth);
            }
            if (aborted(handler)) {
                collector.rollback(handler);
                return;
            }

            final SortedSet<Variable> remainingVars = new TreeSet<>(enumerationVars);
            remainingVars.removeAll(newSplitVars);
            for (final Literal literal : splitModel.getLiterals()) {
                remainingVars.remove(literal.variable());
            }

            final List<Model> newSplitAssignments = collector.rollbackAndReturnModels(solver, handler);
            final SortedSet<Variable> recursiveSplitVars =
                    strategy.splitVarsForRecursionDepth(remainingVars, solver, recursionDepth + 1);
            for (final Model newSplitAssignment : newSplitAssignments) {
                enumerateRecursive(collector, solver, newSplitAssignment, resultSetter, enumerationVars,
                        recursiveSplitVars, recursionDepth + 1);
                if (!collector.commit(handler)) {
                    loadState(solver, state);
                    return;
                }
            }
        } else {
            if (!collector.commit(handler)) {
                loadState(solver, state);
                return;
            }
        }
        loadState(solver, state);
    }

    protected static <R> boolean enumerate(final EnumerationCollector<R> collector, final MiniSat solver,
                                           final Consumer<Tristate> resultSetter, final SortedSet<Variable> variables,
                                           final SortedSet<Variable> additionalVariables, final int maxModels,
                                           final ModelEnumerationHandler handler) {
        final SolverState stateBeforeEnumeration = saveState(solver);
        final LNGIntVector relevantIndices = relevantIndicesFromSolver(variables, solver);
        final LNGIntVector relevantAllIndices =
                relevantAllIndicesFromSolver(variables, additionalVariables, relevantIndices, solver);

        int foundModels = 0;
        boolean proceed = true;
        while (proceed && modelEnumerationSATCall(solver, handler)) {
            final LNGBooleanVector modelFromSolver = solver.underlyingSolver().model();
            if (++foundModels >= maxModels) {
                loadState(solver, stateBeforeEnumeration);
                return false;
            }
            proceed = collector.addModel(modelFromSolver, solver, relevantAllIndices, handler);
            if (modelFromSolver.size() > 0) {
                final LNGIntVector blockingClause = generateBlockingClause(modelFromSolver, relevantIndices);
                solver.underlyingSolver().addClause(blockingClause, null);
                resultSetter.accept(UNDEF);
            } else {
                break;
            }
        }
        loadState(solver, stateBeforeEnumeration);
        return true;
    }

    protected SortedSet<Variable> getVarsForEnumeration(final SortedSet<Variable> knownVariables) {
        final SortedSet<Variable> relevantVars = knownVariables.stream()
                .filter(AbstractModelEnumerationFunction::isNotAuxiliaryVariable)
                .collect(Collectors.toCollection(TreeSet::new));
        return variables == null ? relevantVars :
                relevantVars.stream().filter(variables::contains).collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean isNotAuxiliaryVariable(final Variable var) {
        return !var.name().startsWith(CC_PREFIX) && !var.name().startsWith(PB_PREFIX) &&
                !var.name().startsWith(CNF_PREFIX);
    }

    private static boolean modelEnumerationSATCall(final MiniSat solver, final ModelEnumerationHandler handler) {
        final SATHandler satHandler = handler == null ? null : handler.satHandler();
        final boolean sat = solver.sat(satHandler) == TRUE;
        return !aborted(handler) && sat;
    }

    protected static FormulaFactory factory(final SortedSet<Variable> variables) {
        return variables == null || variables.isEmpty() ? FormulaFactory.caching() : variables.first().factory();
    }

    protected static ModelEnumerationConfig configuration(final SortedSet<Variable> variables,
                                                          final ModelEnumerationConfig config) {
        return config == null ?
                (ModelEnumerationConfig) factory(variables).configurationFor(ConfigurationType.MODEL_ENUMERATION) :
                config;
    }

    private static SolverState saveState(final SATSolver solver) {
        return solver.canSaveLoadState() ? solver.saveState() : null;
    }

    private static void loadState(final SATSolver solver, final SolverState state) {
        if (solver.canSaveLoadState()) {
            solver.loadState(state);
        }
    }
}
