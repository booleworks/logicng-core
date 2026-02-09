// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.OPTIMIZATION_FUNCTION_STARTED;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResultSolver;
import com.booleworks.logicng.encodings.CcIncrementalData;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.OptimizationFoundBetterBoundEvent;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SatCall;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A solver function for computing a model for the formula on the solver which
 * has a global minimum or maximum of satisfied literals.
 * <p>
 * <b>The formula must be satisfiable, otherwise an
 * {@link IllegalArgumentException} will be thrown.</b>
 * <p>
 * Optimization functions are instantiated via their builder {@link #builder()}.
 * @version 3.0.0
 * @since 2.0.0
 */
public class OptimizationFunction implements SolverFunction<Model> {

    protected static final String SEL_PREFIX = "@SEL_OPT_";

    protected final Collection<? extends Literal> literals;
    protected final SortedSet<Variable> resultModelVariables;
    protected final boolean maximize;

    protected OptimizationFunction(final Collection<? extends Literal> literals,
                                   final Collection<Variable> additionalVariables, final boolean maximize) {
        this.literals = literals;
        resultModelVariables = new TreeSet<>(additionalVariables);
        for (final Literal lit : literals) {
            resultModelVariables.add(lit.variable());
        }
        this.maximize = maximize;
    }

    /**
     * Returns the builder for this function.
     * @return the builder
     */
    public static Builder builder() {
        return new OptimizationFunction.Builder();
    }

    /**
     * Returns an optimization function which maximizes the given set of
     * literals.
     * @param literals the literals to maximize
     * @return the solver function
     */
    public static OptimizationFunction maximize(final Collection<? extends Literal> literals) {
        return new Builder().literals(literals).maximize().build();
    }

    /**
     * Returns an optimization function which minimizes the given set of
     * literals.
     * @param literals the literals to minimize
     * @return the solver function
     */
    public static OptimizationFunction minimize(final Collection<? extends Literal> literals) {
        return new Builder().literals(literals).minimize().build();
    }

    @Override
    public LngResult<Model> apply(
            final SatSolver solver, final ComputationHandler handler) {
        final SolverState initialState = solver.saveState();
        final LngResult<Model> model = maximize(solver, handler);
        solver.loadState(initialState);
        return model;
    }

    protected LngResult<Model> maximize(
            final SatSolver solver, final ComputationHandler handler) {
        if (!handler.shouldResume(OPTIMIZATION_FUNCTION_STARTED)) {
            return LngResult.canceled(OPTIMIZATION_FUNCTION_STARTED);
        }
        final FormulaFactory f = solver.getFactory();
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
        Model lastResultModel;
        Model currentSelectorModel;
        try (final SatCall satCall = solver.satCall().handler(handler).solve()) {
            if (!satCall.getSatResult().isSuccess()) {
                return LngResult.canceled(satCall.getSatResult().getCancelCause());
            }
            if (!satCall.getSatResult().getResult()) {
                throw new IllegalArgumentException("The given formula must be satisfiable");
            }
            lastResultModel = satCall.model(resultModelVariables);
            currentSelectorModel = satCall.model(selectors);
            if (currentSelectorModel.positiveVariables().size() == selectors.size()) {
                // all optimization literals satisfied -- no need for further
                // optimization
                return LngResult.of(satCall.model(resultModelVariables));
            }
        }
        int currentBound = currentSelectorModel.positiveVariables().size();
        if (currentBound == 0) {
            solver.add(f.cc(CType.GE, 1, selectors));
            try (final SatCall satCall = solver.satCall().handler(handler).solve()) {
                final LngResult<Boolean> satResult = satCall.getSatResult();
                if (!satResult.isSuccess()) {
                    return LngResult.partial(lastResultModel, satResult.getCancelCause());
                } else if (!satResult.getResult()) {
                    return LngResult.of(lastResultModel);
                } else {
                    lastResultModel = satCall.model(resultModelVariables);
                    currentSelectorModel = satCall.model(selectors);
                    currentBound = currentSelectorModel.positiveVariables().size();
                }
            }
        }
        final Formula cc = f.cc(CType.GE, currentBound + 1, selectors);
        assert cc instanceof CardinalityConstraint;
        final CcIncrementalData<EncodingResultSolver> incrementalData =
                solver.addIncrementalCc((CardinalityConstraint) cc);
        while (true) {
            try (final SatCall satCall = solver.satCall().handler(handler).solve()) {
                final LngResult<Boolean> satResult = satCall.getSatResult();
                if (!satResult.isSuccess()) {
                    return LngResult.partial(lastResultModel, satResult.getCancelCause());
                } else if (!satResult.getResult()) {
                    return LngResult.of(lastResultModel);
                }
                lastResultModel = satCall.model(resultModelVariables);
                final OptimizationFoundBetterBoundEvent betterBoundEvent =
                        new OptimizationFoundBetterBoundEvent(() -> satCall.model(resultModelVariables));
                if (!handler.shouldResume(betterBoundEvent)) {
                    return LngResult.partial(lastResultModel, betterBoundEvent);
                }
                currentSelectorModel = satCall.model(selectors);
                currentBound = currentSelectorModel.positiveVariables().size();
                if (currentBound == selectors.size()) {
                    return LngResult.of(lastResultModel);
                }
            }
            incrementalData.newLowerBoundForSolver(currentBound + 1);
        }
    }

    /**
     * The builder for an optimization function.
     */
    public static final class Builder {
        private Collection<? extends Literal> literals;
        private Collection<Variable> additionalVariables = new TreeSet<>();
        private boolean maximize = true;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the set of literals that should be optimized s.t. the number of
         * satisfied literals is maximized or minimized.
         * @param literals the set of literals
         * @return the current builder
         */
        public Builder literals(final Collection<? extends Literal> literals) {
            this.literals = literals;
            return this;
        }

        /**
         * Sets the set of literals that should be optimized s.t. the number of
         * satisfied literals is maximized or minimized.
         * @param literals the set of literals
         * @return the current builder
         */
        public Builder literals(final Literal... literals) {
            this.literals = Arrays.asList(literals);
            return this;
        }

        /**
         * Sets an additional set of variables which should occur in the
         * resulting model.
         * @param variables the additional variables for the resulting model
         * @return the current builder
         */
        public Builder additionalVariables(final Collection<Variable> variables) {
            additionalVariables = variables;
            return this;
        }

        /**
         * Sets an additional set of variables which should occur in every
         * model.
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
         * Builds the optimization function with the current builder's
         * configuration.
         * @return the optimization function
         */
        public OptimizationFunction build() {
            return new OptimizationFunction(literals, additionalVariables, maximize);
        }
    }
}
