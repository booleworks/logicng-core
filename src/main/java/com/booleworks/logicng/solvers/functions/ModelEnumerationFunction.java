// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.handlers.events.SimpleEvent.MODEL_ENUMERATION_COMMIT;
import static com.booleworks.logicng.handlers.events.SimpleEvent.MODEL_ENUMERATION_ROLLBACK;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.modelenumeration.AbstractModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollector;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A solver function for enumerating models on the solver.
 * <p>
 * Model enumeration functions are instantiated via their builder
 * {@link Builder}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class ModelEnumerationFunction extends AbstractModelEnumerationFunction<List<Model>> {

    ModelEnumerationFunction(final SortedSet<Variable> variables, final SortedSet<Variable> additionalVariables,
                             final ModelEnumerationConfig config) {
        super(variables, additionalVariables, configuration(variables, config));
    }

    /**
     * Constructs a new model enumeration function with the given set of
     * variables.
     * @param variables the variables for the enumeration
     * @return the builder for the function
     */
    public static Builder builder(final Collection<Variable> variables) {
        return new Builder(variables);
    }

    /**
     * Constructs a new model enumeration function with the given set of
     * variables.
     * @param variables the variables for the enumeration
     * @return the builder for the function
     */
    public static Builder builder(final Variable[] variables) {
        return new Builder(new TreeSet<>(asList(variables)));
    }

    @Override
    protected EnumerationCollector<List<Model>> newCollector(final FormulaFactory f,
                                                             final SortedSet<Variable> knownVariables,
                                                             final SortedSet<Variable> dontCareVariablesNotOnSolver,
                                                             final SortedSet<Variable> additionalVariablesNotOnSolver) {
        return new ModelEnumerationCollector(f, dontCareVariablesNotOnSolver, additionalVariablesNotOnSolver);
    }

    /**
     * The builder for a model enumeration function.
     */
    public static class Builder {
        private final SortedSet<Variable> variables;
        private SortedSet<Variable> additionalVariables;
        private ModelEnumerationConfig configuration;

        /**
         * Constructs a new model enumeration function with the given set of
         * variables.
         * @param variables the variables for the enumeration
         */
        Builder(final Collection<Variable> variables) {
            this.variables = new TreeSet<>(variables);
        }

        /**
         * Sets an additional set of variables which should occur in every
         * model. Only set this field if 'variables' is non-empty.
         * @param variables the additional variables for each model
         * @return the current builder
         */
        public Builder additionalVariables(final Collection<Variable> variables) {
            additionalVariables = new TreeSet<>(variables);
            return this;
        }

        /**
         * Sets an additional set of variables which should occur in every
         * model. Only set this field if 'variables' is non-empty.
         * @param variables the additional variables for each model
         * @return the current builder
         */
        public Builder additionalVariables(final Variable... variables) {
            additionalVariables = new TreeSet<>(asList(variables));
            return this;
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
        public ModelEnumerationFunction build() {
            return new ModelEnumerationFunction(variables, additionalVariables, configuration);
        }
    }

    static class ModelEnumerationCollector implements EnumerationCollector<List<Model>> {
        private final List<Model> committedModels = new ArrayList<>();
        private final List<List<Literal>> uncommittedModels = new ArrayList<>();
        private final List<List<Literal>> baseModels;
        private final SortedSet<Literal> additionalVariablesNotOnSolver;

        public ModelEnumerationCollector(final FormulaFactory f, final SortedSet<Variable> dontCareVariablesNotOnSolver,
                                         final SortedSet<Variable> additionalVariablesNotOnSolver) {
            baseModels = getCartesianProduct(dontCareVariablesNotOnSolver);
            this.additionalVariablesNotOnSolver = new TreeSet<>();
            for (final Variable addVar : additionalVariablesNotOnSolver) {
                this.additionalVariablesNotOnSolver.add(addVar.negate(f));
            }
        }

        @Override
        public boolean addModel(final LNGBooleanVector modelFromSolver, final SATSolver solver,
                                final LNGIntVector relevantAllIndices,
                                final ComputationHandler handler) {
            if (handler.shouldResume(new EnumerationFoundModelsEvent(baseModels.size()))) {
                final Model model =
                        new Model(solver.underlyingSolver().convertInternalModel(modelFromSolver, relevantAllIndices));
                final List<Literal> modelLiterals = new ArrayList<>(additionalVariablesNotOnSolver);
                modelLiterals.addAll(model.getLiterals());
                uncommittedModels.add(modelLiterals);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean commit(final ComputationHandler handler) {
            committedModels.addAll(expandUncommittedModels());
            uncommittedModels.clear();
            return handler.shouldResume(MODEL_ENUMERATION_COMMIT);
        }

        @Override
        public boolean rollback(final ComputationHandler handler) {
            uncommittedModels.clear();
            return handler.shouldResume(MODEL_ENUMERATION_ROLLBACK);
        }

        @Override
        public List<Model> rollbackAndReturnModels(final SATSolver solver, final ComputationHandler handler) {
            final List<Model> modelsToReturn = uncommittedModels.stream().map(Model::new).collect(Collectors.toList());
            rollback(handler);
            return modelsToReturn;
        }

        @Override
        public List<Model> getResult() {
            return committedModels;
        }

        private List<Model> expandUncommittedModels() {
            final List<Model> expanded = new ArrayList<>(baseModels.size());
            for (final List<Literal> baseModel : baseModels) {
                for (final List<Literal> uncommittedModel : uncommittedModels) {
                    final List<Literal> completeModel = new ArrayList<>(baseModel.size() + uncommittedModel.size());
                    completeModel.addAll(baseModel);
                    completeModel.addAll(uncommittedModel);
                    expanded.add(new Model(completeModel));
                }
            }
            return expanded;
        }

        /**
         * Returns the Cartesian product for the given variables, i.e. all
         * combinations of literals are generated with each variable occurring
         * positively and negatively.
         * @param variables the variables, must not be {@code null}
         * @return the Cartesian product
         */
        static List<List<Literal>> getCartesianProduct(final SortedSet<Variable> variables) {
            List<List<Literal>> result = singletonList(emptyList());
            for (final Variable var : variables) {
                final List<List<Literal>> extended = new ArrayList<>(result.size() * 2);
                for (final List<Literal> literals : result) {
                    extended.add(extendedByLiteral(literals, var));
                    extended.add(extendedByLiteral(literals, var.negate(var.factory())));
                }
                result = extended;
            }
            return result;
        }

        private static List<Literal> extendedByLiteral(final List<Literal> literals, final Literal lit) {
            final ArrayList<Literal> extended = new ArrayList<>(literals);
            extended.add(lit);
            return extended;
        }
    }
}
