package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.CspAssignment;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Map;

public class OrderDecoder {

    private final Csp csp;
    private final OrderEncodingResult encodingInformation;

    public OrderDecoder(final Csp csp, final OrderEncodingResult encodingResult) {
        this.csp = csp;
        this.encodingInformation = encodingResult;
    }

    public CspAssignment decode(final Assignment model) {
        final CspAssignment result = new CspAssignment();
        for (final IntegerVariable v : this.csp.getIntegerVariables()) {
            final int value = decodeIntVar(v, model);
            result.addIntAssignment(v, value);
        }
        for (final Variable v : this.csp.getBooleanVariables()) {
            if (model.positiveVariables().contains(v)) {
                result.addPos(v);
            }
            final Literal negV = v.negate(this.csp.getCspFactory().getFormulaFactory());
            if (model.negativeLiterals().contains(v)) {
                result.addNeg(negV);
            }
        }
        return result;
    }

    private int decodeIntVar(final IntegerVariable var, final Assignment model) {
        final IntegerDomain domain = var.getDomain();
        final int lb = domain.lb();
        final int ub = domain.ub();
        int value = ub;
        final Map<Integer, Variable> varMap = this.encodingInformation.getVariableMap().get(var);
        for (int c = lb; c < ub; c++) {
            if (domain.contains(c)) {
                final Variable satVar = varMap.get(c - lb);
                if (model.positiveVariables().contains(satVar)) {
                    value = c;
                    break;
                }
            }
        }
        return value;
    }
}
