package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CspBackbone {
    private static final CspBackbone UNSAT_BACKBONE = new CspBackbone(null, null, Backbone.unsatBackbone());

    private final Map<IntegerVariable, Integer> mandatory;
    private final Map<IntegerVariable, SortedSet<Integer>> forbidden;
    private final Backbone booleanBackbone;

    private CspBackbone(final Map<IntegerVariable, Integer> mandatory,
                        final Map<IntegerVariable, SortedSet<Integer>> forbidden, final Backbone booleanBackbone) {
        this.mandatory = mandatory;
        this.forbidden = forbidden;
        this.booleanBackbone = booleanBackbone;
    }

    public static CspBackbone satBackbone(final Map<IntegerVariable, Integer> mandatory,
                                          final Map<IntegerVariable, SortedSet<Integer>> forbidden,
                                          final Backbone booleanBackbone) {
        return new CspBackbone(mandatory, forbidden, booleanBackbone);
    }

    public static CspBackbone unsatBackbone() {
        return UNSAT_BACKBONE;
    }

    public boolean isMandatory(final IntegerVariable v, final int value) {
        return mandatory.get(v) == value;
    }

    public boolean isForbidden(final IntegerVariable v, final int value) {
        return (mandatory.containsKey(v) && mandatory.get(v) != value)
                || (forbidden.containsKey(v) && forbidden.get(v).contains(value));
    }

    public boolean isOptional(final IntegerVariable v, final int value) {
        return !isMandatory(v, value) && !isForbidden(v, value);
    }

    public Map<IntegerVariable, Integer> getMandatory() {
        return Collections.unmodifiableMap(mandatory);
    }

    public Map<IntegerVariable, SortedSet<Integer>> getForbidden() {
        return Collections.unmodifiableMap(forbidden);
    }

    public Backbone getBooleanBackbone() {
        return booleanBackbone;
    }

    public static CspBackbone calculateBackbone(final SatSolver solver,
                                                final Csp csp,
                                                final CspEncodingContext context,
                                                final EncodingResult result, final CspFactory cf) {
        return calculateBackbone(solver, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(), context,
                result, cf);
    }

    public static CspBackbone calculateBackbone(final SatSolver solver,
                                                final Collection<IntegerVariable> integerVariables,
                                                final Collection<Variable> booleanVariables,
                                                final CspEncodingContext context,
                                                final EncodingResult result, final CspFactory cf) {
        final Map<IntegerVariable, Map<Variable, Integer>> valueHooks =
                CspValueHook.encodeValueHooks(integerVariables, context, result, cf);
        return calculateBackbone(solver, integerVariables, booleanVariables, context, valueHooks, cf);
    }

    public static CspBackbone calculateBackbone(final SatSolver solver,
                                                final Csp csp,
                                                final CspEncodingContext context,
                                                final Map<IntegerVariable, Map<Variable, Integer>> valueHooks,
                                                final CspFactory cf) {
        return calculateBackbone(solver, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(), context,
                valueHooks, cf);
    }

    public static CspBackbone calculateBackbone(final SatSolver solver,
                                                final Collection<IntegerVariable> integerVariables,
                                                final Collection<Variable> booleanVariables,
                                                final CspEncodingContext context,
                                                final Map<IntegerVariable, Map<Variable, Integer>> valueHooks,
                                                final CspFactory cf) {
        final List<Variable> hookVariables = valueHooks.values().stream()
                .flatMap(m -> m.keySet().stream()).collect(Collectors.toList());
        final List<Variable> relevantVariables = new ArrayList<>(booleanVariables);
        relevantVariables.addAll(hookVariables);
        final Backbone backbone = solver.backbone(relevantVariables);
        if (!backbone.isSat()) {
            return CspBackbone.unsatBackbone();
        }
        final Backbone filteredBackbone = filterBackbone(backbone, booleanVariables);
        final CspBackbone cspBackbone = new CspBackbone(new LinkedHashMap<>(), new LinkedHashMap<>(), filteredBackbone);
        for (final IntegerVariable iv : integerVariables) {
            valueHooks.get(iv).forEach((k, v) -> {
                if (backbone.getPositiveBackbone().contains(k)) {
                    cspBackbone.mandatory.put(iv, v);
                }
            });
        }
        for (final IntegerVariable iv : integerVariables) {
            if (!cspBackbone.mandatory.containsKey(iv)) {
                final SortedSet<Integer> forbidden = new TreeSet<>();
                valueHooks.get(iv).forEach((k, v) -> {
                    if (backbone.getNegativeBackbone().contains(k)) {
                        forbidden.add(v);
                    }
                });
                if (!forbidden.isEmpty()) {
                    cspBackbone.forbidden.put(iv, forbidden);
                }
            }
        }
        return cspBackbone;

    }

    private static Backbone filterBackbone(final Backbone backbone, final Collection<Variable> relevantVariables) {
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
