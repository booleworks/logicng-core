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
    protected final Collection<IntegerVariable> integerVariables;
    protected final Collection<Variable> booleanVariables;
    protected final CspFactory cf;

    protected CspBackboneGeneration(final BackboneType type,
                                    final Collection<IntegerVariable> integerVariables,
                                    final Collection<Variable> booleanVariables,
                                    final CspFactory cf) {
        this.type = type;
        this.integerVariables = integerVariables;
        this.booleanVariables = booleanVariables;
        this.cf = cf;
    }

    /**
     * Construct a new backbone generation instance respecting a set of boolean
     * and integer variables.
     * @param type             the backbone computation type
     * @param integerVariables relevant integer variables
     * @param booleanVariables relevant boolean variables
     * @param cf               the factory
     * @return the generation instance
     */
    public static CspBackboneGeneration fromVariables(final BackboneType type,
                                                      final Collection<IntegerVariable> integerVariables,
                                                      final Collection<Variable> booleanVariables,
                                                      final CspFactory cf) {
        return new CspBackboneGeneration(type, integerVariables, booleanVariables, cf);
    }

    /**
     * Construct a new backbone generation instance respecting a set of boolean
     * and integer variables.
     * @param integerVariables relevant integer variables
     * @param booleanVariables relevant boolean variables
     * @param cf               the factory
     * @return the generation instance
     */
    public static CspBackboneGeneration fromVariables(final Collection<IntegerVariable> integerVariables,
                                                      final Collection<Variable> booleanVariables,
                                                      final CspFactory cf) {
        return new CspBackboneGeneration(BackboneType.POSITIVE_AND_NEGATIVE, integerVariables, booleanVariables, cf);
    }

    /**
     * Construct a new backbone generation instance respecting the variables of a CSP problem.
     * @param type the backbone computation type
     * @param csp  the CSP problem
     * @param cf   the factory
     * @return the generation instance
     */
    public static CspBackboneGeneration fromCsp(final BackboneType type, final Csp csp, final CspFactory cf) {
        return new CspBackboneGeneration(type, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(), cf);
    }

    /**
     * Construct a new backbone generation instance respecting the variables of a CSP problem.
     * @param csp the CSP problem
     * @param cf  the factory
     * @return the generation instance
     */
    public static CspBackboneGeneration fromCsp(final Csp csp, final CspFactory cf) {
        return new CspBackboneGeneration(BackboneType.POSITIVE_AND_NEGATIVE, csp.getVisibleIntegerVariables(),
                csp.getVisibleBooleanVariables(), cf);
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
        return compute(solver, valueHooks, context, handler);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the
     * solver.
     * @param solver     the solver with the csp on it
     * @param valueHooks the value hooks
     * @param context    the encoding context
     * @return the backbone
     */
    public CspBackbone compute(final SatSolver solver, final CspValueHookMap valueHooks,
                               final CspEncodingContext context) {
        return compute(solver, valueHooks, context, NopHandler.get()).getResult();
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the
     * solver.
     * @param solver     the solver with the csp on it
     * @param valueHooks the value hooks
     * @param context    the encoding context
     * @param handler    handler for processing events
     * @return the backbone
     */
    public LngResult<CspBackbone> compute(final SatSolver solver, final CspValueHookMap valueHooks,
                                          final CspEncodingContext context, final ComputationHandler handler) {
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
}
