// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.handlers.events.SimpleEvent.MODEL_ENUMERATION_COMMIT;
import static com.booleworks.logicng.handlers.events.SimpleEvent.MODEL_ENUMERATION_ROLLBACK;
import static java.util.Arrays.asList;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;
import com.booleworks.logicng.knowledgecompilation.bdds.BddFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.solvers.SatSolver;
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
public class ModelEnumerationToBddFunction extends AbstractModelEnumerationFunction<Bdd> {

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
    protected EnumerationCollector<Bdd> newCollector(final FormulaFactory f, final SortedSet<Variable> knownVariables,
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

    static class BddModelEnumerationCollector implements EnumerationCollector<Bdd> {
        private final BddKernel kernel;
        private Bdd committedModels;
        private final List<Model> uncommittedModels = new ArrayList<>();
        private final int dontCareFactor;

        public BddModelEnumerationCollector(final FormulaFactory f, final SortedSet<Variable> variables,
                                            final SortedSet<Variable> knownVariables,
                                            final int numberDontCareVariablesNotOnSolver) {
            final List<Variable> sortedVariables =
                    variables != null ? new ArrayList<>(variables) : new ArrayList<>(knownVariables);
            final int numVars = sortedVariables.size();
            kernel = new BddKernel(f, sortedVariables, numVars * 30, numVars * 50);
            committedModels = BddFactory.build(f, f.falsum(), kernel);
            dontCareFactor = (int) Math.pow(2, numberDontCareVariablesNotOnSolver);
        }

        @Override
        public LngEvent addModel(final LngBooleanVector modelFromSolver, final SatSolver solver,
                                 final LngIntVector relevantAllIndices,
                                 final ComputationHandler handler) {
            final EnumerationFoundModelsEvent event = new EnumerationFoundModelsEvent(dontCareFactor);
            final Model model =
                    new Model(solver.getUnderlyingSolver().convertInternalModel(modelFromSolver, relevantAllIndices));
            uncommittedModels.add(model);
            return handler.shouldResume(event) ? null : event;
        }

        @Override
        public LngEvent commit(final ComputationHandler handler) {
            for (final Model uncommittedModel : uncommittedModels) {
                committedModels = committedModels.or(model2Bdd(uncommittedModel));
            }
            uncommittedModels.clear();
            return handler.shouldResume(MODEL_ENUMERATION_COMMIT) ? null : MODEL_ENUMERATION_COMMIT;
        }

        private Bdd model2Bdd(final Model model) {
            Bdd bdd = BddFactory.build(kernel.factory(), kernel.factory().verum(), kernel);
            for (final Literal literal : model.getLiterals()) {
                bdd = bdd.and(BddFactory.build(kernel.factory(), literal, kernel));
            }
            return bdd;
        }

        @Override
        public LngEvent rollback(final ComputationHandler handler) {
            uncommittedModels.clear();
            return handler.shouldResume(MODEL_ENUMERATION_ROLLBACK) ? null : MODEL_ENUMERATION_ROLLBACK;
        }

        @Override
        public List<Model> rollbackAndReturnModels(final SatSolver solver, final ComputationHandler handler) {
            final List<Model> modelsToReturn = new ArrayList<>(uncommittedModels);
            rollback(handler);
            return modelsToReturn;
        }

        @Override
        public Bdd getResult() {
            return committedModels;
        }
    }
}
