// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.datastructures.Tristate.TRUE;
import static com.booleworks.logicng.datastructures.Tristate.UNDEF;
import static com.booleworks.logicng.handlers.Handler.start;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.relevantAllIndicesFromSolver;
import static com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon.relevantIndicesFromSolver;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationCommon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * A solver function for enumerating models on the solver.
 * <p>
 * Model enumeration functions are instantiated via their builder {@link #builder()}.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class ModelEnumerationFunction implements SolverFunction<List<Assignment>> {

    private final ModelEnumerationHandler handler;
    private final Collection<Variable> variables;
    private final Collection<Variable> additionalVariables;

    private ModelEnumerationFunction(final ModelEnumerationHandler handler, final Collection<Variable> variables,
                                     final Collection<Variable> additionalVariables) {
        this.handler = handler;
        this.variables = variables;
        this.additionalVariables = additionalVariables;
    }

    /**
     * Returns the builder for this function.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<Assignment> apply(final MiniSat solver, final Consumer<Tristate> resultSetter) {
        start(handler);
        final List<Assignment> models = new ArrayList<>();
        SolverState stateBeforeEnumeration = null;
        if (solver.canSaveLoadState()) {
            stateBeforeEnumeration = solver.saveState();
        }
        final LNGIntVector relevantIndices = relevantIndicesFromSolver(variables, solver);
        final LNGIntVector relevantAllIndices = relevantAllIndicesFromSolver(variables, additionalVariables, relevantIndices, solver);

        boolean proceed = true;
        while (proceed && modelEnumerationSATCall(solver, handler)) {
            final LNGBooleanVector modelFromSolver = solver.underlyingSolver().model();
            final Assignment model = solver.createAssignment(modelFromSolver, relevantAllIndices);
            models.add(model);
            proceed = handler == null || handler.foundModel(model);
            if (model.size() > 0) {
                final LNGIntVector blockingClause = ModelEnumerationCommon.generateBlockingClause(modelFromSolver, relevantIndices);
                solver.underlyingSolver().addClause(blockingClause, null);
                resultSetter.accept(UNDEF);
            } else {
                break;
            }
        }
        if (solver.canSaveLoadState()) {
            solver.loadState(stateBeforeEnumeration);
        }
        return models;
    }

    private boolean modelEnumerationSATCall(final MiniSat solver, final ModelEnumerationHandler handler) {
        if (handler == null) {
            return solver.sat((SATHandler) null) == TRUE;
        }
        final Tristate tristate = solver.sat(handler.satHandler());
        return !handler.aborted() && tristate == TRUE;
    }

    /**
     * The builder for a model enumeration function.
     */
    public static class Builder {
        private ModelEnumerationHandler handler;
        private Collection<Variable> variables;
        private Collection<Variable> additionalVariables;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the model enumeration handler for this function
         * @param handler the handler, may be {@code null}
         * @return the current builder
         */
        public Builder handler(final ModelEnumerationHandler handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets the set of variables over which the model enumeration should iterate.
         * @param variables the set of variables
         * @return the current builder
         */
        public Builder variables(final Collection<Variable> variables) {
            this.variables = variables;
            return this;
        }

        /**
         * Sets the set of variables over which the model enumeration should iterate.
         * @param variables the set of variables
         * @return the current builder
         */
        public Builder variables(final Variable... variables) {
            this.variables = Arrays.asList(variables);
            return this;
        }

        /**
         * Sets an additional set of variables which should occur in every model.
         * @param variables the additional variables for each model
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
         * Builds the model enumeration function with the current builder's configuration.
         * @return the model enumeration function
         */
        public ModelEnumerationFunction build() {
            return new ModelEnumerationFunction(handler, variables, additionalVariables);
        }
    }
}
