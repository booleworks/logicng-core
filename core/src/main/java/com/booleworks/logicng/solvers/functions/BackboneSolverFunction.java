// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import com.booleworks.logicng.datastructures.Backbone;
import com.booleworks.logicng.datastructures.BackboneType;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;

import java.util.Arrays;
import java.util.Collection;

/**
 * A solver function which computes a backbone for the formula on the solver.
 * <p>
 * Backbone functions are instantiated via their builder {@link #builder(Collection)}.
 * @version 3.0.0
 * @since 2.0.0
 */
public class BackboneSolverFunction implements SolverFunction<Backbone> {

    protected final Collection<Variable> variables;
    protected final BackboneType type;

    protected BackboneSolverFunction(final Collection<Variable> variables, final BackboneType type) {
        this.variables = variables;
        this.type = type;
    }

    /**
     * Returns the builder for this function.
     * @param variables the relevant variables for the backbone computation
     * @return the builder
     */
    public static Builder builder(final Collection<Variable> variables) {
        return new Builder(variables);
    }

    @Override
    public LngResult<Backbone> apply(final SatSolver solver, final ComputationHandler handler) {
        return solver.getUnderlyingSolver().computeBackbone(variables, type, handler);
    }

    /**
     * The builder for a backbone function.
     */
    public static final class Builder {

        private Collection<Variable> variables;
        private BackboneType type = BackboneType.POSITIVE_AND_NEGATIVE;

        private Builder(final Collection<Variable> variables) {
            this.variables = variables;
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
        public BackboneSolverFunction build() {
            return new BackboneSolverFunction(variables, type);
        }
    }
}
