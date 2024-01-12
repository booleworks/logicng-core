// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.handlers.Handler.aborted;
import static com.booleworks.logicng.handlers.Handler.start;
import static com.booleworks.logicng.handlers.OptimizationHandler.satHandler;

import com.booleworks.logicng.cardinalityconstraints.CCIncrementalData;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.OptimizationHandler;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATCall;
import com.booleworks.logicng.solvers.SolverState;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * A solver function for computing a model for the formula on the solver
 * which has a global minimum or maximum of satisfied literals. If the formula
 * is UNSAT or the optimization handler aborted the computation, {@code null}
 * will be returned.
 * <p>
 * Optimization functions are instantiated via their builder {@link #builder()}.
 * @version 2.1.0
 * @since 2.0.0
 */
public final class OptimizationFunction implements SolverFunction<Assignment> {

    private static final String SEL_PREFIX = "@SEL_OPT_";

    private final Collection<? extends Literal> literals;
    private final SortedSet<Variable> resultModelVariables;
    private final boolean maximize;
    private final OptimizationHandler handler;

    private OptimizationFunction(final Collection<? extends Literal> literals, final Collection<Variable> additionalVariables, final boolean maximize,
                                 final OptimizationHandler handler) {
        this.literals = literals;
        resultModelVariables = new TreeSet<>(additionalVariables);
        for (final Literal lit : literals) {
            resultModelVariables.add(lit.variable());
        }
        this.maximize = maximize;
        this.handler = handler;
    }

    /**
     * Returns the builder for this function.
     * @return the builder
     */
    public static Builder builder() {
        return new OptimizationFunction.Builder();
    }

    /**
     * Returns an optimization function which maximizes the given set of literals.
     * @param literals the literals to maximize
     * @return the solver function
     */
    public static OptimizationFunction maximize(final Collection<? extends Literal> literals) {
        return new Builder().literals(literals).maximize().build();
    }

    /**
     * Returns an optimization function which minimizes the given set of literals.
     * @param literals the literals to minimize
     * @return the solver function
     */
    public static OptimizationFunction minimize(final Collection<? extends Literal> literals) {
        return new Builder().literals(literals).minimize().build();
    }

    @Override
    public Assignment apply(final MiniSat solver, final Consumer<Tristate> resultSetter) {
        final SolverState initialState = solver.saveState();
        final Assignment model = maximize(solver);
        solver.loadState(initialState);
        return model;
    }

    private Assignment maximize(final MiniSat solver) {
        start(handler);
        final FormulaFactory f = solver.factory();
        LNGBooleanVector internalModel;
        final Map<Variable, Literal> selectorMap = new TreeMap<>();
        for (final Literal lit : literals) {
            final Variable selVar = f.variable(SEL_PREFIX + selectorMap.size());
            selectorMap.put(selVar, lit);
        }
        final Set<Variable> selectors = selectorMap.keySet();
        if (maximize) {
            selectorMap.forEach((selVar, lit) -> solver.add(f.or(selVar.negate(f), lit)));
            selectorMap.forEach((selVar, lit) -> solver.add(f.or(lit.negate(f), selVar)));
        } else {
            selectorMap.forEach((selVar, lit) -> solver.add(f.or(selVar.negate(f), lit.negate(f))));
            selectorMap.forEach((selVar, lit) -> solver.add(f.or(lit, selVar)));
        }
        Assignment currentModel;
        try (final SATCall satCall = solver.satCall().handler(satHandler(handler)).start()) {
            if (satCall.getSatState() != Tristate.TRUE || aborted(handler)) {
                return null;
            }
            internalModel = solver.underlyingSolver().model();
            currentModel = satCall.model(selectors);
        }
        int currentBound = currentModel.positiveVariables().size();
        if (currentBound == 0) {
            solver.add(f.cc(CType.GE, 1, selectors));
            try (final SATCall satCall = solver.satCall().handler(satHandler(handler)).start()) {
                final Tristate sat = satCall.getSatState();
                if (aborted(handler)) {
                    return null;
                } else if (sat == Tristate.FALSE) {
                    return mkResultModel(solver, internalModel);
                } else {
                    internalModel = solver.underlyingSolver().model();
                    currentModel = satCall.model(selectors);
                    currentBound = currentModel.positiveVariables().size();
                }
            }
        } else if (currentBound == selectors.size()) {
            return mkResultModel(solver, internalModel);
        }
        final Formula cc = f.cc(CType.GE, currentBound + 1, selectors);
        assert cc instanceof CardinalityConstraint;
        final CCIncrementalData incrementalData = solver.addIncrementalCC((CardinalityConstraint) cc);
        while (true) {
            try (final SATCall satCall = solver.satCall().handler(satHandler(handler)).start()) {
                if (aborted(handler)) {
                    return null;
                }
                if (satCall.getSatState() == Tristate.FALSE) {
                    break;
                }
                final LNGBooleanVector modelCopy = new LNGBooleanVector(solver.underlyingSolver().model());
                if (handler != null && !handler.foundBetterBound(() -> mkResultModel(solver, modelCopy))) {
                    return null;
                }
                internalModel = modelCopy;
                currentModel = satCall.model(selectors);
                currentBound = currentModel.positiveVariables().size();
                if (currentBound == selectors.size()) {
                    return mkResultModel(solver, internalModel);
                }
                incrementalData.newLowerBoundForSolver(currentBound + 1);
            }
        }
        return mkResultModel(solver, internalModel);
    }

    private Assignment mkResultModel(final MiniSat solver, final LNGBooleanVector internalModel) {
        final LNGIntVector relevantIndices = new LNGIntVector(resultModelVariables.size());
        for (final Variable var : resultModelVariables) {
            relevantIndices.push(solver.underlyingSolver().idxForName(var.name()));
        }
        return solver.createAssignment(internalModel, relevantIndices);
    }

    /**
     * The builder for an optimization function.
     */
    public static class Builder {
        private Collection<? extends Literal> literals;
        private Collection<Variable> additionalVariables = new TreeSet<>();
        private boolean maximize = true;
        private OptimizationHandler handler = null;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the set of literals that should be optimized s.t. the number of satisfied literals is maximized or minimized.
         * @param literals the set of literals
         * @return the current builder
         */
        public Builder literals(final Collection<? extends Literal> literals) {
            this.literals = literals;
            return this;
        }

        /**
         * Sets the set of literals that should be optimized s.t. the number of satisfied literals is maximized or minimized.
         * @param literals the set of literals
         * @return the current builder
         */
        public Builder literals(final Literal... literals) {
            this.literals = Arrays.asList(literals);
            return this;
        }

        /**
         * Sets an additional set of variables which should occur in the resulting model.
         * @param variables the additional variables for the resulting model
         * @return the current builder
         */
        public Builder additionalVariables(final Collection<Variable> variables) {
            additionalVariables = variables;
            return this;
        }

        /**
         * Sets an additional set of variables which should occur in every model.
         * @param variables the additional variables for each model
         * @return the current builder
         */
        public Builder additionalVariables(final Variable... variables) {
            additionalVariables = Arrays.asList(variables);
            return this;
        }

        /**
         * Sets the optimization goal to minimize.
         * @return the current builder
         */
        public Builder minimize() {
            maximize = false;
            return this;
        }

        /**
         * Sets the optimization goal to maximize.
         * @return the current builder
         */
        public Builder maximize() {
            maximize = true;
            return this;
        }

        /**
         * Sets the handler for the optimization.
         * @param handler the handler
         * @return the current builder
         */
        public Builder handler(final OptimizationHandler handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Builds the optimization function with the current builder's configuration.
         * @return the optimization function
         */
        public OptimizationFunction build() {
            return new OptimizationFunction(literals, additionalVariables, maximize, handler);
        }
    }
}
