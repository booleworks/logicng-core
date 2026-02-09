// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspBackbone;
import com.booleworks.logicng.csp.datastructures.CspValueHookMap;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Backbone;
import com.booleworks.logicng.datastructures.BackboneType;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.BackboneSolverFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class for {@link CspBackbone} computations.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspBackboneGeneration {
    protected final BackboneType type;
    protected final List<IntegerVariable> integerVariables;
    protected final List<Variable> booleanVariables;
    protected final CspFactory cf;

    protected CspBackboneGeneration(final Builder builder) {
        this.type = builder.type;
        this.integerVariables = new ArrayList<>(builder.integerVariables);
        this.booleanVariables = new ArrayList<>(builder.booleanVariables);
        this.cf = builder.cf;
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * This function adds new value hooks to the solver. There are alternative
     * functions that take existing value hooks as argument.
     * @param solver  the solver with the csp on it
     * @param context the encoding context
     * @param result  the destination for the new value hooks
     * @return the backbone
     */
    public CspBackbone compute(final SatSolver solver, final CspEncodingContext context, final EncodingResult result) {
        return compute(solver, context, result, NopHandler.get()).getResult();
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * This function adds new value hooks to the solver. There are alternative
     * functions that take existing value hooks as argument.
     * @param solver  the solver with the csp on it
     * @param context the encoding context
     * @param result  the destination for the new value hooks
     * @param handler handler for processing events
     * @return the backbone
     */
    public LngResult<CspBackbone> compute(final SatSolver solver, final CspEncodingContext context,
                                          final EncodingResult result, final ComputationHandler handler) {
        final CspValueHookMap valueHooks =
                CspValueHookEncoding.encodeValueHooks(integerVariables, context, result, cf);
        return compute(solver, valueHooks, handler);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the
     * solver.
     * @param solver     the solver with the csp on it
     * @param valueHooks the value hooks
     * @return the backbone
     */
    public CspBackbone compute(final SatSolver solver, final CspValueHookMap valueHooks) {
        return compute(solver, valueHooks, NopHandler.get()).getResult();
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the
     * solver.
     * @param solver     the solver with the csp on it
     * @param valueHooks the value hooks
     * @param handler    handler for processing events
     * @return the backbone
     */
    public LngResult<CspBackbone> compute(final SatSolver solver, final CspValueHookMap valueHooks,
                                          final ComputationHandler handler) {
        final List<Variable> hookVariables = valueHooks.getHooks().values().stream()
                .flatMap(m -> m.keySet().stream()).collect(Collectors.toList());
        final List<Variable> relevantVariables = new ArrayList<>(booleanVariables);
        relevantVariables.addAll(hookVariables);
        final BackboneSolverFunction backboneFunction =
                BackboneSolverFunction.builder(relevantVariables).type(type).build();
        final LngResult<Backbone> backboneResult = solver.execute(backboneFunction, handler);
        if (!backboneResult.isSuccess()) {
            return LngResult.canceled(backboneResult.getCancelCause());
        }
        final Backbone backbone = backboneResult.getResult();
        if (!backbone.isSat()) {
            return LngResult.of(CspBackbone.unsatBackbone());
        }
        final Backbone filteredBackbone = filterBackbone(backbone, booleanVariables);
        final Map<IntegerVariable, Integer> mandatoryMap = new LinkedHashMap<>();
        final Map<IntegerVariable, SortedSet<Integer>> forbiddenMap = new LinkedHashMap<>();
        for (final IntegerVariable iv : integerVariables) {
            valueHooks.get(iv).forEach((k, v) -> {
                if (backbone.getPositiveBackbone().contains(k)) {
                    mandatoryMap.put(iv, v);
                }
            });
        }
        for (final IntegerVariable iv : integerVariables) {
            if (!mandatoryMap.containsKey(iv)) {
                final SortedSet<Integer> forbidden = new TreeSet<>();
                valueHooks.get(iv).forEach((k, v) -> {
                    if (backbone.getNegativeBackbone().contains(k)) {
                        forbidden.add(v);
                    }
                });
                if (!forbidden.isEmpty()) {
                    forbiddenMap.put(iv, forbidden);
                }
            }
        }
        return LngResult.of(CspBackbone.satBackbone(mandatoryMap, forbiddenMap, filteredBackbone));
    }

    protected static Backbone filterBackbone(final Backbone backbone, final Collection<Variable> relevantVariables) {
        return Backbone.satBackbone(
                backbone.getPositiveBackbone().stream().filter(relevantVariables::contains)
                        .collect(Collectors.toCollection(TreeSet::new)),
                backbone.getNegativeBackbone().stream().filter(relevantVariables::contains)
                        .collect(Collectors.toCollection(TreeSet::new)),
                backbone.getOptionalVariables().stream().filter(relevantVariables::contains)
                        .collect(Collectors.toCollection(TreeSet::new))
        );
    }

    /**
     * Get a backbone function builder from the sets of relevant variables.
     * @param cf               the factory
     * @param integerVariables the relevant integer variables
     * @param booleanVariables the relevant boolean variables
     * @return a backbone function builder
     */
    public static Builder builderFromVariables(final CspFactory cf, final Collection<IntegerVariable> integerVariables,
                                               final Collection<Variable> booleanVariables) {
        return new Builder(cf, integerVariables, booleanVariables);
    }

    /**
     * Get a backbone function builder from the variables of {@code csp}.
     * @param cf  the factory
     * @param csp the CSP problem
     * @return a backbone function builder
     */
    public static Builder builderFromCsp(final CspFactory cf, final Csp csp) {
        return new Builder(cf, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables());
    }

    /**
     * Backbone function builder.
     */
    public static final class Builder {
        private BackboneType type = BackboneType.POSITIVE_AND_NEGATIVE;
        private Collection<IntegerVariable> integerVariables;
        private Collection<Variable> booleanVariables;
        private CspFactory cf;

        private Builder(final CspFactory cf, final Collection<IntegerVariable> integerVariables,
                        final Collection<Variable> booleanVariables) {
            this.cf = cf;
            this.integerVariables = integerVariables;
            this.booleanVariables = booleanVariables;
        }

        /**
         * Sets the factory.
         * @param cf the factory
         * @return this builder
         */
        public Builder factory(final CspFactory cf) {
            this.cf = cf;
            return this;
        }

        /**
         * Sets the relevant integer variables.
         * @param integerVariables the relevant integer variables
         * @return this builder
         */
        public Builder integerVariables(final Collection<IntegerVariable> integerVariables) {
            this.integerVariables = integerVariables;
            return this;
        }

        /**
         * Sets the relevant boolean variables.
         * @param booleanVariables the relevant boolean variables
         * @return this builder
         */
        public Builder booleanVariables(final Collection<Variable> booleanVariables) {
            this.booleanVariables = booleanVariables;
            return this;
        }

        /**
         * Set the type of the backbone computation
         * @param type the backbone type
         * @return this builder
         */
        public Builder backboneType(final BackboneType type) {
            this.type = type;
            return this;
        }

        /**
         * Builds the backbone function.
         * @return the backbone function
         */
        public CspBackboneGeneration build() {
            return new CspBackboneGeneration(this);
        }
    }
}
