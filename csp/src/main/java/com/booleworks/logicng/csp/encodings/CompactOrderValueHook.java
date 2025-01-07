package com.booleworks.logicng.csp.encodings;

import static com.booleworks.logicng.csp.encodings.CspEncodingContext.CSP_AUX_LNG_VARIABLE;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Value hook and value projection for compact order encoding.
 */
public class CompactOrderValueHook {
    private CompactOrderValueHook() {
    }

    /**
     * Encodes values hooks for the given variable.
     * @param v       the variable
     * @param context the encoding context
     * @param result  the destination for the hooks
     * @param cf      the factory
     * @return a mapping of boolean variables to integer value they represent
     */
    public static Map<Variable, Integer> encodeValueHooks(final IntegerVariable v,
                                                          final CompactOrderEncodingContext context,
                                                          final EncodingResult result, final CspFactory cf) {
        assert (context.isEncoded(v));
        final FormulaFactory f = cf.getFormulaFactory();

        final IntegerVariable adjustedV = context.getAdjustedVariableOrSelf(v);
        final List<IntegerVariable> variableDigits = context.getDigits(adjustedV);
        final List<Map<Integer, Variable>> digitHooks = new ArrayList<>();
        for (final IntegerVariable vd : variableDigits) {
            final Map<Integer, Variable> reversedMap = new HashMap<>();
            OrderValueHook.encodeValueHooks(vd, context.getOrderContext(), result, cf)
                    .forEach((key, val) -> reversedMap.put(val, key));
            digitHooks.add(reversedMap);
        }

        final Map<Variable, Integer> map = new HashMap<>();
        final Iterator<Integer> values = v.getDomain().iterator();
        while (values.hasNext()) {
            final int realValue = values.next();
            final int offsetValue = realValue - context.getOffset(adjustedV);
            final Variable h = f.newAuxVariable(CSP_AUX_LNG_VARIABLE);
            map.put(h, realValue);

            final List<Integer> valueDigits = CompactCSPReduction.intToDigits(offsetValue, context.getBase());
            final ArrayList<Variable> hooks = new ArrayList<>();
            for (int i = 0; i < digitHooks.size(); ++i) {
                int dv = 0;
                if (i < valueDigits.size()) {
                    dv = valueDigits.get(i);
                }
                hooks.add(digitHooks.get(i).get(dv));
            }
            final LngVector<Literal> clause1 = new LngVector<>(hooks.size() + 1);
            //(h0 & h1 & ...) -> h == ~h0 | ~h1 | ... | h
            // h -> (h0 & h1 & ...) == (~h | h0) & (~h | h1) ...
            clause1.push(h);
            for (final Variable dh : hooks) {
                clause1.push(dh.negate(f));
                result.addClause(h.negate(f), dh);
            }
            result.addClause(clause1);
        }
        return map;
    }

    /**
     * Returns an assignment of boolean variables that represent a specific integer value of an integer variable.
     * @param v       the integer variable
     * @param value   the value
     * @param context the encoding context
     * @param cf      the factory
     * @return assignment of boolean variables representing the given value for the given integer variable
     */
    public static List<Literal> calculateValueProjection(final IntegerVariable v, final int value,
                                                         final CompactOrderEncodingContext context,
                                                         final CspFactory cf) {
        assert (v.getDomain().contains(value));
        assert (context.isEncoded(v));

        final IntegerVariable adjustedV = context.getAdjustedVariableOrSelf(v);
        final int offsetValue = value - context.getOffset(adjustedV);
        final List<Integer> valueDigits = CompactCSPReduction.intToDigits(offsetValue, context.getBase());
        final List<IntegerVariable> variableDigits = context.getDigits(adjustedV);
        final List<Literal> result = new ArrayList<>();
        for (int i = 0; i < variableDigits.size(); ++i) {
            int dv = 0;
            if (i < valueDigits.size()) {
                dv = valueDigits.get(i);
            }
            result.addAll(OrderValueHook.calculateValueProjection(variableDigits.get(i), dv,
                    context.getOrderContext(), cf));
        }
        return result;
    }
}
