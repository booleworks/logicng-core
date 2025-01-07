package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.encodings.CompactOrderEncodingContext;
import com.booleworks.logicng.csp.encodings.CompactOrderValueHook;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderValueHook;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CspValueHook {


    public static Map<Variable, Integer> encodeValueHooks(final IntegerVariable v, final CspEncodingContext context,
                                                          final EncodingResult result, final CspFactory cf) {
        switch (context.getAlgorithm()) {
            case Order:
                return OrderValueHook.encodeValueHooks(v, (OrderEncodingContext) context, result, cf);
            case CompactOrder:
                return CompactOrderValueHook.encodeValueHooks(v, (CompactOrderEncodingContext) context, result, cf);
            default:
                throw new RuntimeException("Unsupported Algorithm for Value Hooks");
        }
    }

    public static Map<IntegerVariable, Map<Variable, Integer>> encodeValueHooks(
            final Collection<IntegerVariable> variables, final CspEncodingContext context, final EncodingResult result,
            final CspFactory cf) {
        final Map<IntegerVariable, Map<Variable, Integer>> map = new HashMap<>();
        for (final IntegerVariable v : variables) {
            map.put(v, encodeValueHooks(v, context, result, cf));
        }
        return map;
    }

    public static Map<IntegerVariable, Map<Variable, Integer>> encodeValueHooks(final Csp csp,
                                                                                final CspEncodingContext context,
                                                                                final EncodingResult result,
                                                                                final CspFactory cf) {
        return encodeValueHooks(
                csp.getVisibleIntegerVariables().stream().filter(csp.getInternalIntegerVariables()::contains)
                        .collect(Collectors.toList()),
                context, result, cf);
    }

    public static List<Literal> calculateValueProjection(final IntegerVariable v, final int value,
                                                         final CspEncodingContext context,
                                                         final CspFactory cf) {
        switch (context.getAlgorithm()) {
            case Order:
                return OrderValueHook.calculateValueProjection(v, value, (OrderEncodingContext) context, cf);
            case CompactOrder:
                return CompactOrderValueHook.calculateValueProjection(v, value, (CompactOrderEncodingContext) context,
                        cf);
            default:
                throw new RuntimeException("Unsupported Algorithm for Value Hooks");
        }
    }
}
