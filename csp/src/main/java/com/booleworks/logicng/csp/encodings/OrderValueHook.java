package com.booleworks.logicng.csp.encodings;

import static com.booleworks.logicng.csp.encodings.CspEncodingContext.CSP_AUX_LNG_VARIABLE;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Value hook and value projection for order encoding.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class OrderValueHook {
    private OrderValueHook() {
    }

    /**
     * Encodes values hooks for the given variable.
     * @param v       the variable
     * @param context the encoding context
     * @param result  the destination for the hooks
     * @param cf      the factory
     * @return a mapping of boolean variables to integer value they represent
     */
    public static Map<Variable, Integer> encodeValueHooks(final IntegerVariable v, final OrderEncodingContext context,
                                                          final EncodingResult result, final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final Map<Variable, Integer> map = new HashMap<>();
        assert context.getVariableMap().containsKey(v);
        final Variable[] orderVars = context.getVariableMap().get(v);
        final IntegerDomain domain = v.getDomain();

        Variable previousVar = null;
        int index = 0;
        int c = domain.lb();
        while (c < domain.ub()) {
            if (v.getDomain().contains(c)) {
                final Variable orderVar = orderVars[index];
                final Variable hookVar = f.newAuxVariable(CSP_AUX_LNG_VARIABLE); //TODO: track?
                if (previousVar == null) {
                    // v0 <-> h_0
                    result.addClause(orderVar.negate(f), hookVar);
                    result.addClause(hookVar.negate(f), orderVar);
                } else {
                    // (~v0 & v1) <-> h  == (v0 | ~v1 | h1) & (~h1 | ~vo)  & (~h1 | v1)
                    result.addClause(previousVar, orderVar.negate(f), hookVar);
                    result.addClause(hookVar.negate(f), previousVar.negate(f));
                    result.addClause(hookVar.negate(f), orderVar);
                }
                map.put(hookVar, c);
                previousVar = orderVar;
                ++index;
            }
            ++c;
        }

        final Variable hookVar = f.newAuxVariable(CSP_AUX_LNG_VARIABLE);
        map.put(hookVar, domain.ub());
        if (previousVar == null) {
            // true <-> h0 == h0
            result.addClause(hookVar);
        } else {
            // ~v_n <-> h_ub == (v_n | h_ub) & (~h_up | ~v_n )
            result.addClause(previousVar, hookVar);
            result.addClause(hookVar.negate(f), previousVar.negate(f));
        }
        return map;
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
    public static List<Literal> calculateValueProjection(final IntegerVariable v, final int value,
                                                         final OrderEncodingContext context, final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        assert context.getVariableMap().containsKey(v);
        final Variable[] orderVars = context.getVariableMap().get(v);
        final IntegerDomain domain = v.getDomain();

        Variable previousVar = null;
        int index = 0;
        int c = domain.lb();
        while (c < domain.ub()) {
            if (v.getDomain().contains(c)) {
                final Variable orderVar = orderVars[index];
                if (c == value) {
                    if (previousVar == null) {
                        // v0
                        return List.of(orderVar);
                    } else {
                        // ~v0 & v1
                        return List.of(previousVar.negate(f), orderVar);
                    }
                }
                previousVar = orderVar;
                ++index;
            }
            ++c;
        }

        assert value == domain.ub();
        if (previousVar == null) {
            // This should only happen if the integer variable has a domain with one value.
            // In this case, it can only be this value, and we don't need additional values.
            return List.of();
        } else {
            // ~v_n <-> h_ub == (v_n | h_ub) & (~h_up | ~v_n )
            return List.of(previousVar.negate(f));
        }
    }
}
