// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BACKBONE_COMPUTATION_STARTED;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;

import java.util.Arrays;
import java.util.Collection;

/**
 * A solver function which computes a backbone for the formula on the solver.
 * <p>
 * Backbone functions are instantiated via their builder {@link #builder()}.
 * @version 2.1.0
 * @since 2.0.0
 */
public final class BackboneFunction implements SolverFunction<Backbone> {

    private final ComputationHandler handler;
    private final Collection<Variable> variables;
    private final BackboneType type;

    private BackboneFunction(final ComputationHandler handler, final Collection<Variable> variables, final BackboneType type) {
        this.handler = handler;
        this.variables = variables;
        this.type = type;
    }

    /**
     * Returns the builder for this function.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Backbone apply(final SATSolver solver) {
        handler.shouldResume(BACKBONE_COMPUTATION_STARTED);
        final SolverState stateBeforeBackbone = solver.saveState();
        final Backbone backbone = solver.underlyingSolver().computeBackbone(variables, type, handler);
        solver.loadState(stateBeforeBackbone);
        return backbone;
    }

    /**
     * The builder for a backbone function.
     */
    public static class Builder {

        private ComputationHandler handler = NopHandler.get();
        private Collection<Variable> variables;
        private BackboneType type = BackboneType.POSITIVE_AND_NEGATIVE;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the SAT handler for this function.
         * @param handler the handler
         * @return the current builder
         */
        public Builder handler(final ComputationHandler handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets the variables which are relevant for the backbone computation.
         * @param variables the variables
         * @return the current builder
         */
        public Builder variables(final Collection<Variable> variables) {
            this.variables = variables;
            return this;
        }

        /**
         * Sets the variables which are relevant for the backbone computation.
         * @param variables the variables
         * @return the current builder
         */
        public Builder variables(final Variable... variables) {
            this.variables = Arrays.asList(variables);
            return this;
        }

        /**
         * Sets the type of backbone which should be computed (default:
         * POSITIVE_AND_NEGATIVE).
         * @param type the backbone type
         * @return the current builder
         */
        public Builder type(final BackboneType type) {
            this.type = type;
            return this;
        }

        /**
         * Builds the backbone function with the current builder's
         * configuration.
         * @return the backbone function
         */
        public BackboneFunction build() {
            return new BackboneFunction(handler, variables, type);
        }
    }
}
