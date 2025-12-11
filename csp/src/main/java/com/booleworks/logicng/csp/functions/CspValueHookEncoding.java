// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspValueHookMap;
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

/**
 * A class providing functions that can be used to add special variables to the
 * encoding of a CSP problem that can be used to read or restrict the value of
 * integer variables.
 * <ul>
 *     <li>Value Hooks: Embeds auxiliary variables that correspond directly with
 *     a value of the integer variable.</li>
 *     <li>Value Projection: A fixed value for an integer variable is translated
 *     to a boolean assignment of the variables of the encoding.</li>
 * </ul>
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CspValueHookEncoding {
    private CspValueHookEncoding() {
    }

    /**
     * Encodes values hooks for the given variable.
     * @param v       the variable
     * @param context the encoding context
     * @param result  the destination for the hooks
     * @param cf      the factory
     * @return a mapping of boolean variables to integer value they represent
     */
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

    /**
     * Encodes value hooks for a set of integer variables.
     * @param variables set of integer variables
     * @param context   the encoding context
     * @param result    the destination for the hooks
     * @param cf        the factory
     * @return a mapping from integer variables to a mapping from boolean
     * variables to their represented integer value
     */
    public static CspValueHookMap encodeValueHooks(
            final Collection<IntegerVariable> variables, final CspEncodingContext context, final EncodingResult result,
            final CspFactory cf) {
        final Map<IntegerVariable, Map<Variable, Integer>> map = new HashMap<>();
        for (final IntegerVariable v : variables) {
            map.put(v, encodeValueHooks(v, context, result, cf));
        }
        return new CspValueHookMap(map);
    }

    /**
     * Encodes value hooks for all integer variables of a csp.
     * @param csp     the csp
     * @param context the encoding context
     * @param result  the destination for the hooks
     * @param cf      the factory
     * @return a mapping from integer variables to a mapping from boolean
     * variables to their represented integer value
     */
    public static CspValueHookMap encodeValueHooks(final Csp csp, final CspEncodingContext context,
                                                   final EncodingResult result, final CspFactory cf) {
        return encodeValueHooks(
                csp.getVisibleIntegerVariables().stream().filter(csp.getInternalIntegerVariables()::contains)
                        .collect(Collectors.toList()),
                context, result, cf);
    }

    /**
     * Returns an assignment of boolean variables that represent a specific
     * integer value of an integer variable.
     * @param v       the integer variable
     * @param value   the value
     * @param context the encoding context
     * @param cf      the factory
     * @return assignment of boolean variables representing the given value for
     * the given integer variable
     */
    public static List<Literal> computeRestrictionAssignments(final IntegerVariable v, final int value,
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
