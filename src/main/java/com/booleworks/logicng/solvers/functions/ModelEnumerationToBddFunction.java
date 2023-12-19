// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static java.util.Arrays.asList;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.BDDFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.functions.modelenumeration.AbstractModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollector;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A solver function for enumerating models on the solver and storing the result
 * in a BDD. If used with a subset of the original formula's variables this
 * performs an existential quantifier elimination (or projection) of the
 * original formula into a BDD.
 * <p>
 * Model enumeration functions are instantiated via their builder
 * {@link Builder}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class ModelEnumerationToBddFunction extends AbstractModelEnumerationFunction<BDD> {

    ModelEnumerationToBddFunction(final SortedSet<Variable> variables, final ModelEnumerationConfig config) {
        super(variables, Collections.emptySortedSet(), configuration(variables, config));
    }

    /**
     * Constructs a new BDD model enumeration function with the given set of
     * variables.
     * @param variables the variables for the enumeration
     * @return the builder for the function
     */
    public static Builder builder(final Collection<Variable> variables) {
        return new Builder(variables);
    }

    /**
     * Constructs a new BDD model enumeration function with the given set of
     * variables.
     * @param variables the variables for the enumeration
     * @return the builder for the function
     */
    public static Builder builder(final Variable[] variables) {
        return new Builder(new TreeSet<>(asList(variables)));
    }

    @Override
    protected EnumerationCollector<BDD> newCollector(final FormulaFactory f, final SortedSet<Variable> knownVariables,
                                                     final SortedSet<Variable> dontCareVariablesNotOnSolver,
                                                     final SortedSet<Variable> additionalVariablesNotOnSolver) {
        return new BddModelEnumerationCollector(f, variables, knownVariables, dontCareVariablesNotOnSolver.size());
    }

    /**
     * The builder for a BDD model enumeration function.
     */
    public static class Builder {
        private final SortedSet<Variable> variables;
        private ModelEnumerationConfig configuration;

        /**
         * Constructs a new BDD model enumeration function with the given set of
         * variables.
         * @param variables the variables for the enumeration
         */
        Builder(final Collection<Variable> variables) {
            this.variables = new TreeSet<>(variables);
        }

        /**
         * Sets the configuration for the model enumeration split algorithm.
         * @param configuration the configuration
         * @return the current builder
         */
        public Builder configuration(final ModelEnumerationConfig configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Builds the model enumeration function with the current builder's
         * configuration.
         * @return the model enumeration function
         */
        public ModelEnumerationToBddFunction build() {
            return new ModelEnumerationToBddFunction(variables, configuration);
        }
    }

    static class BddModelEnumerationCollector implements EnumerationCollector<BDD> {
        private final BDDKernel kernel;
        private BDD committedModels;
        private final List<Model> uncommittedModels = new ArrayList<>();
        private final int dontCareFactor;

        public BddModelEnumerationCollector(final FormulaFactory f, final SortedSet<Variable> variables,
                                            final SortedSet<Variable> knownVariables,
                                            final int numberDontCareVariablesNotOnSolver) {
            final List<Variable> sortedVariables =
                    variables != null ? new ArrayList<>(variables) : new ArrayList<>(knownVariables);
            final int numVars = sortedVariables.size();
            kernel = new BDDKernel(f, sortedVariables, numVars * 30, numVars * 50);
            committedModels = BDDFactory.build(f, f.falsum(), kernel);
            dontCareFactor = (int) Math.pow(2, numberDontCareVariablesNotOnSolver);
        }

        @Override
        public boolean addModel(final LNGBooleanVector modelFromSolver, final MiniSat solver,
                                final LNGIntVector relevantAllIndices, final ModelEnumerationHandler handler) {
            if (handler == null || handler.foundModels(dontCareFactor)) {
                final Model model = solver.createModel(modelFromSolver, relevantAllIndices);
                uncommittedModels.add(model);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean commit(final ModelEnumerationHandler handler) {
            for (final Model uncommittedModel : uncommittedModels) {
                committedModels = committedModels.or(model2Bdd(uncommittedModel));
            }
            uncommittedModels.clear();
            return handler == null || handler.commit();
        }

        private BDD model2Bdd(final Model model) {
            BDD bdd = BDDFactory.build(kernel.factory(), kernel.factory().verum(), kernel);
            for (final Literal literal : model.getLiterals()) {
                bdd = bdd.and(BDDFactory.build(kernel.factory(), literal, kernel));
            }
            return bdd;
        }

        @Override
        public boolean rollback(final ModelEnumerationHandler handler) {
            uncommittedModels.clear();
            return handler == null || handler.rollback();
        }

        @Override
        public List<Model> rollbackAndReturnModels(final MiniSat solver, final ModelEnumerationHandler handler) {
            final List<Model> modelsToReturn = new ArrayList<>(uncommittedModels);
            rollback(handler);
            return modelsToReturn;
        }

        @Override
        public BDD getResult() {
            return committedModels;
        }
    }
}
