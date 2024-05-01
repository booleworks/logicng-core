package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.CspAssignment;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Map;

public class OrderDecoding {
    public static CspAssignment decode(final Assignment model, final Collection<IntegerVariable> variables, final CspEncodingContext context, final CspFactory cf) {
        final CspAssignment result = new CspAssignment();
        for (final IntegerVariable v : variables) {
            final int value = decodeIntVar(v, model, context);
            result.addIntAssignment(cf.getUnboundedVariableOf(v), value);
        }
        return result;
    }

    public static CspAssignment decode(final Assignment model, final Csp csp, final CspEncodingContext context, final CspFactory cf) {
        final CspAssignment result = new CspAssignment();
        for (final IntegerVariable v : csp.getIntegerVariables()) {
            final int value = decodeIntVar(v, model, context);
            result.addIntAssignment(cf.getUnboundedVariableOf(v), value);
        }
        for (final Variable v : csp.getBooleanVariables()) {
            if (model.positiveVariables().contains(v)) {
                result.addPos(v);
            }
            final Literal negV = v.negate(cf.formulaFactory());
            if (model.negativeLiterals().contains(v)) {
                result.addNeg(negV);
            }
        }
        return result;
    }

    static int decodeIntVar(final IntegerVariable var, final Assignment model, final CspEncodingContext context) {
        final IntegerDomain domain = var.getDomain();
        final int lb = domain.lb();
        final int ub = domain.ub();
        int value = ub;
        final Map<Integer, Variable> varMap = context.getVariableMap().get(var);
        int index = 0;
        for (int c = lb; c < ub; c++) {
            if (domain.contains(c)) {
                final Variable satVar = varMap.get(index);
                if (satVar != null && model.positiveVariables().contains(satVar)) {
                    value = c;
                    break;
                }
                ++index;
            }
        }
        return value;
    }
}
