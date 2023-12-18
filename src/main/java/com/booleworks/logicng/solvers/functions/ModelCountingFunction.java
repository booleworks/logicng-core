// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static java.util.Arrays.asList;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.functions.modelenumeration.AbstractModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollector;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A solver function for counting models on the solver.
 * <p>
 * Model enumeration functions are instantiated via their builder
 * {@link Builder}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class ModelCountingFunction extends AbstractModelEnumerationFunction<BigInteger> {

    ModelCountingFunction(final SortedSet<Variable> variables, final ModelEnumerationConfig config) {
        super(variables, Collections.emptySortedSet(), configuration(variables, config));
    }

    /**
     * Constructs a new model counting function with the given set of variables.
     * @param variables the variables for the enumeration
     * @return the builder for the function
     */
    public static Builder builder(final Collection<Variable> variables) {
        return new Builder(variables);
    }

    /**
     * Constructs a new model counting function with the given set of variables.
     * @param variables the variables for the enumeration
     * @return the builder for the function
     */
    public static Builder builder(final Variable[] variables) {
        return new Builder(new TreeSet<>(asList(variables)));
    }

    @Override
    protected EnumerationCollector<BigInteger> newCollector(final FormulaFactory f,
                                                            final SortedSet<Variable> knownVariables,
                                                            final SortedSet<Variable> dontCareVariablesNotOnSolver,
                                                            final SortedSet<Variable> additionalVariablesNotOnSolver) {
        return new ModelCountCollector(dontCareVariablesNotOnSolver.size());
    }

    /**
     * The builder for a model counting function.
     */
    public static class Builder {
        private final SortedSet<Variable> variables;
        private ModelEnumerationConfig configuration;

        /**
         * Constructs a new model counting function with the given set of
         * variables.
         * @param variables the variables for the enumeration
         */
        Builder(final Collection<Variable> variables) {
            this.variables = new TreeSet<>(variables);
        }

        /**
         * Sets the configuration for the underlying model enumeration split
         * algorithm.
         * @param configuration the configuration
         * @return the current builder
         */
        public Builder configuration(final ModelEnumerationConfig configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Builds the model counting function with the current builder's
         * configuration.
         * @return the model counting function
         */
        public ModelCountingFunction build() {
            return new ModelCountingFunction(variables, configuration);
        }
    }

    static class ModelCountCollector implements EnumerationCollector<BigInteger> {
        private BigInteger committedCount = BigInteger.ZERO;
        private final List<LNGBooleanVector> uncommittedModels = new ArrayList<>(100);
        private final List<LNGIntVector> uncommittedIndices = new ArrayList<>(100);
        private final BigInteger dontCareFactor;

        public ModelCountCollector(final int numberDontCareVariablesNotOnSolver) {
            dontCareFactor = BigInteger.valueOf(2).pow(numberDontCareVariablesNotOnSolver);
        }

        @Override
        public boolean addModel(final LNGBooleanVector modelFromSolver, final MiniSat solver,
                                final LNGIntVector relevantAllIndices,
                                final ModelEnumerationHandler handler) {
            if (handler == null || handler.foundModels(dontCareFactor.intValue())) {
                uncommittedModels.add(modelFromSolver);
                uncommittedIndices.add(relevantAllIndices);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean commit(final ModelEnumerationHandler handler) {
            committedCount = committedCount.add(BigInteger.valueOf(uncommittedModels.size()).multiply(dontCareFactor));
            clearUncommitted();
            return handler == null || handler.commit();
        }

        @Override
        public boolean rollback(final ModelEnumerationHandler handler) {
            clearUncommitted();
            return handler == null || handler.rollback();
        }

        @Override
        public List<Model> rollbackAndReturnModels(final MiniSat solver, final ModelEnumerationHandler handler) {
            final List<Model> modelsToReturn = new ArrayList<>(uncommittedModels.size());
            for (int i = 0; i < uncommittedModels.size(); i++) {
                modelsToReturn.add(solver.createModel(uncommittedModels.get(i), uncommittedIndices.get(i)));
            }
            rollback(handler);
            return modelsToReturn;
        }

        @Override
        public BigInteger getResult() {
            return committedCount;
        }

        private void clearUncommitted() {
            uncommittedModels.clear();
            uncommittedIndices.clear();
        }
    }
}
