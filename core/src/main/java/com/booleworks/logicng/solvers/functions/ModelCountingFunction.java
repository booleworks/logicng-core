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
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.SatSolver;
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

    protected ModelCountingFunction(final SortedSet<Variable> variables, final ModelEnumerationConfig config) {
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
    public static final class Builder {
        private final SortedSet<Variable> variables;
        private ModelEnumerationConfig configuration;

        /**
         * Constructs a new model counting function with the given set of
         * variables.
         * @param variables the variables for the enumeration
         */
        private Builder(final Collection<Variable> variables) {
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

    static final class ModelCountCollector implements EnumerationCollector<BigInteger> {
        private BigInteger committedCount = BigInteger.ZERO;
        private final List<LngBooleanVector> uncommittedModels = new ArrayList<>(100);
        private final List<LngIntVector> uncommittedIndices = new ArrayList<>(100);
        private final BigInteger dontCareFactor;

        public ModelCountCollector(final int numberDontCareVariablesNotOnSolver) {
            dontCareFactor = BigInteger.valueOf(2).pow(numberDontCareVariablesNotOnSolver);
        }

        @Override
        public LngEvent addModel(final LngBooleanVector modelFromSolver, final SatSolver solver,
                                 final LngIntVector relevantAllIndices, final ComputationHandler handler) {
            final EnumerationFoundModelsEvent event = new EnumerationFoundModelsEvent(dontCareFactor.intValue());
            uncommittedModels.add(modelFromSolver);
            uncommittedIndices.add(relevantAllIndices);
            return handler.shouldResume(event) ? null : event;
        }

        @Override
        public LngEvent commit(final ComputationHandler handler) {
            committedCount = committedCount.add(BigInteger.valueOf(uncommittedModels.size()).multiply(dontCareFactor));
            clearUncommitted();
            return handler.shouldResume(MODEL_ENUMERATION_COMMIT) ? null : MODEL_ENUMERATION_COMMIT;
        }

        @Override
        public LngEvent rollback(final ComputationHandler handler) {
            clearUncommitted();
            return handler.shouldResume(MODEL_ENUMERATION_ROLLBACK) ? null : MODEL_ENUMERATION_ROLLBACK;
        }

        @Override
        public LngResult<List<Model>> rollbackAndReturnModels(final SatSolver solver, final ComputationHandler handler) {
            final List<Model> modelsToReturn = new ArrayList<>(uncommittedModels.size());
            for (int i = 0; i < uncommittedModels.size(); i++) {
                modelsToReturn.add(new Model(solver.getUnderlyingSolver().convertInternalModel(uncommittedModels.get(i),
                        uncommittedIndices.get(i))));
            }
            final LngEvent cancelCause = rollback(handler);
            return cancelCause == null ? LngResult.of(modelsToReturn) : LngResult.canceled(cancelCause);
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
