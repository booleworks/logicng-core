package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.CspAssignment;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

public class OrderEncoder {

    private final Csp originalCsp;
    private Csp reducedCsp;
    private final AuxGenerator auxGenerator;

    protected class AuxGenerator {
        int booleanVariables = 0;
        int integerVariables = 0;

        IntegerVariable newAuxIntVariable(final String prefix, final IntegerDomain domain) {
            return IntegerVariable.auxVar(prefix + (++integerVariables), domain);
        }

        Variable newAuxBoolVariable(final String prefix) {
            return originalCsp.getCspFactory().getFormulaFactory().variable(prefix + (++booleanVariables));
        }

        Literal negate(final Variable v) {
            return v.negate(originalCsp.getCspFactory().getFormulaFactory());
        }
    }

    public OrderEncoder(final Csp csp) {
        this.originalCsp = csp;
        this.auxGenerator = new AuxGenerator();
        reducedCsp = null;
    }

    public void encode(final OrderEncodingResult result) {
        if (reducedCsp == null) {
            reducedCsp = OrderReduction.reduce(originalCsp, auxGenerator);
        }
        OrderEncoding.encode(reducedCsp, result);
    }

    public void reduceOnly() {
        reducedCsp = OrderReduction.reduce(originalCsp, auxGenerator);
    }

    public CspAssignment decode(final Assignment model, final OrderEncodingResult result) {
        return OrderDecoding.decode(model, reducedCsp, result);
    }

    public Csp getOriginalCsp() {
        return originalCsp;
    }

    public Csp getReducedCsp() {
        return reducedCsp;
    }
}
