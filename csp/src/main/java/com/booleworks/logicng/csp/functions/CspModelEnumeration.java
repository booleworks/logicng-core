// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Functions for enumerate models from CSP problems.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspModelEnumeration {
    protected final CspFactory cf;
    protected final Collection<IntegerVariable> integerVariables;
    protected final Collection<IntegerVariable> additionalIntegerVariables;
    protected final Collection<Variable> booleanVariables;
    protected final Collection<Variable> additionalBooleanVariables;

    protected CspModelEnumeration(final Builder builder) {
        this.cf = builder.cf;
        this.integerVariables = new ArrayList<>(builder.integerVariables);
        this.additionalIntegerVariables = new ArrayList<>(builder.additionalIntegerVariables);
        this.booleanVariables = new ArrayList<>(builder.booleanVariables);
        this.additionalBooleanVariables = new ArrayList<>(builder.additionalBooleanVariables);
    }

    /**
     * Enumerate models from a solver and a set of relevant integer and boolean
     * variables. The relevant variables are all in the produced models. If a
     * variable was not encoded on the solver, the function will assume that all
     * values of the variable are allowed.
     * @param solver  the solver with the encoded problem
     * @param context the encoding context
     * @return a list with all models for this problem
     */
    public List<CspAssignment> enumerate(final SatSolver solver, final CspEncodingContext context) {
        return enumerate(solver, context, NopHandler.get()).getResult();
    }

    /**
     * Enumerate models from a solver and a set of relevant integer and boolean
     * variables. The relevant variables are all in the produced models. If a
     * variable was not encoded on the solver, the function will assume that all
     * values of the variable are allowed.
     * @param solver  the solver with the encoded problem
     * @param context the encoding context
     * @param handler handler for processing events
     * @return a list with all models for this problem
     */
    public LngResult<List<CspAssignment>> enumerate(final SatSolver solver, final CspEncodingContext context,
                                                    final ComputationHandler handler) {
        final SortedSet<IntegerVariable> intVariablesOnSolver =
                CspUtil.getVariablesOnSolver(solver.getUnderlyingSolver().knownVariables(), integerVariables, context);
        final SortedSet<IntegerVariable> additionalIntVariablesOnSolver =
                CspUtil.getVariablesOnSolver(solver.getUnderlyingSolver().knownVariables(), additionalIntegerVariables,
                        context);
        final SortedSet<IntegerVariable> allIntVariablesOnSolver = new TreeSet<>(intVariablesOnSolver);
        allIntVariablesOnSolver.addAll(additionalIntVariablesOnSolver);
        final List<IntegerVariable> intVariablesNotOnSolver = integerVariables
                .stream()
                .filter(v -> !intVariablesOnSolver.contains(v))
                .collect(Collectors.toList());
        final List<IntegerVariable> additionalIntVariablesNotOnSolver = additionalIntegerVariables
                .stream()
                .filter(v -> !additionalIntVariablesOnSolver.contains(v) && !integerVariables.contains(v))
                .collect(Collectors.toList());
        final List<Variable> allBooleanVars = new ArrayList<>(booleanVariables);
        allBooleanVars.addAll(additionalBooleanVariables);
        final Set<Variable> allVars = context.getEncodingVariables(intVariablesOnSolver);
        allVars.addAll(booleanVariables);
        final Set<Variable> allAdditionalVariables = context.getEncodingVariables(additionalIntVariablesOnSolver);
        allAdditionalVariables.addAll(additionalBooleanVariables);
        final ModelEnumerationFunction meFunction = ModelEnumerationFunction
                .builder(allVars)
                .additionalVariables(allAdditionalVariables)
                .configuration(ModelEnumerationConfig
                        .builder()
                        .strategy(DefaultModelEnumerationStrategy.builder().build())
                        .build())
                .build();
        final LngResult<List<Model>> meResult = solver.execute(meFunction, handler);
        if (!meResult.isSuccess()) {
            return LngResult.canceled(meResult.getCancelCause());
        }
        final List<CspAssignment> decodedModels = meResult.getResult().stream()
                .map(m -> cf.decode(m.toAssignment(), allIntVariablesOnSolver, allBooleanVars, context))
                .map(m -> augmentMissingAdditionalVariables(m, additionalIntVariablesNotOnSolver))
                .collect(Collectors.toList());
        if (intVariablesNotOnSolver.isEmpty() || decodedModels.isEmpty()) {
            return LngResult.of(decodedModels);
        } else {
            return LngResult.of(enumerateDontCareVariables(decodedModels, intVariablesNotOnSolver));
        }
    }

    protected static CspAssignment augmentMissingAdditionalVariables(final CspAssignment decodedModel,
                                                                     final List<IntegerVariable> additionalIntegerVariables) {
        for (final IntegerVariable iv : additionalIntegerVariables) {
            decodedModel.addIntAssignment(iv, iv.getDomain().ub());
        }
        return decodedModel;
    }

    protected static List<CspAssignment> enumerateDontCareVariables(final List<CspAssignment> decodedModels,
                                                                    final List<IntegerVariable> integerVariables) {
        final List<Iterator<Integer>> iterators =
                integerVariables.stream().map(v -> v.getDomain().iterator()).collect(Collectors.toList());
        final List<Integer> values =
                iterators.stream().map(Iterator::next).collect(Collectors.toList()); //Domains cannot be empty
        final List<CspAssignment> models = new ArrayList<>();
        int leaderIndex = 0;
        for (int index = 0; index < integerVariables.size(); ) {
            for (final CspAssignment model : decodedModels) {
                final CspAssignment newModel = new CspAssignment(model);
                for (int i = 0; i < integerVariables.size(); ++i) {
                    newModel.addIntAssignment(integerVariables.get(i), values.get(i));
                }
                models.add(newModel);
            }
            if (iterators.get(index).hasNext()) {
                values.set(index, iterators.get(index).next());
            } else {
                while (index < integerVariables.size() && !iterators.get(index).hasNext()) {
                    ++index;
                }
                if (index == integerVariables.size()) {
                    break;
                }
                values.set(index, iterators.get(index).next());
                for (int i = 0; i < index; ++i) {
                    iterators.set(i, integerVariables.get(i).getDomain().iterator());
                    values.set(i, iterators.get(i).next());
                }
                if (index > leaderIndex) {
                    leaderIndex = index;
                }
                index = 0;
            }
        }
        return models;
    }

    /**
     * Get a model enumeration builder from the variables of {@code csp}.
     * @param cf  the factory
     * @param csp the CSP problem
     * @return the model enumeration builder
     */
    public static Builder builderFromCsp(final CspFactory cf, final Csp csp) {
        return new Builder(cf, csp.getPropagateSubstitutions().getAllOrSelf(csp.getVisibleIntegerVariables()),
                csp.getVisibleBooleanVariables());
    }

    /**
     * Get a model enumeration builder from sets of relevant variables.
     * @param cf               the factory
     * @param integerVariables the relevant integer variables
     * @param booleanVariables the relevant boolean variables
     * @return the model enumeration builder
     */
    public static Builder builderFromVariables(final CspFactory cf, final Collection<IntegerVariable> integerVariables,
                                               final Collection<Variable> booleanVariables) {
        return new Builder(cf, integerVariables, booleanVariables);
    }

    /**
     * Model enumeration builder.
     */
    public static final class Builder {
        private CspFactory cf;
        private Collection<IntegerVariable> integerVariables;
        private Collection<IntegerVariable> additionalIntegerVariables = new ArrayList<>();
        private Collection<Variable> booleanVariables;
        private Collection<Variable> additionalBooleanVariables = new ArrayList<>();

        private Builder(final CspFactory cf, final Collection<IntegerVariable> integerVariables,
                        final Collection<Variable> booleanVariables) {
            this.cf = cf;
            this.integerVariables = integerVariables;
            this.booleanVariables = booleanVariables;
        }

        /**
         * Sets the factory used for the enumeration
         * @param cf the factory
         * @return this builder
         */
        public Builder factory(final CspFactory cf) {
            this.cf = cf;
            return this;
        }

        /**
         * Sets the relevant integer variables used in the enumeration.
         * @param integerVariables the relevant integer variables
         * @return this builder
         */
        public Builder integerVariables(final Collection<IntegerVariable> integerVariables) {
            this.integerVariables = integerVariables;
            return this;
        }

        /**
         * Sets the relevant boolean variables used in the enumeration.
         * @param booleanVariables the relevant boolean variables
         * @return this builder
         */
        public Builder booleanVariables(final Collection<Variable> booleanVariables) {
            this.booleanVariables = booleanVariables;
            return this;
        }

        /**
         * Sets additional integer variables that should be contained in the
         * enumerated model but are not enumerated.
         * @param additionalIntegerVariables the additional integer variables
         * @return this builder
         */
        public Builder additionalIntegerVariables(final Collection<IntegerVariable> additionalIntegerVariables) {
            this.additionalIntegerVariables = additionalIntegerVariables;
            return this;
        }

        /**
         * Sets additional boolean variables that should be contained in the
         * enumerated model but are not enumerated.
         * @param additionalBooleanVariables the additional boolean variables
         * @return this builder
         */
        public Builder additionalBooleanVariables(final Collection<Variable> additionalBooleanVariables) {
            this.additionalBooleanVariables = additionalBooleanVariables;
            return this;
        }

        /**
         * Builds the enumeration function.
         * @return the enumeration function
         */
        public CspModelEnumeration build() {
            return new CspModelEnumeration(this);
        }
    }
}
