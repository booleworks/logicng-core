package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.LinearExpression;

public final class NegationFunction extends UnaryFunction {

    public NegationFunction(final CspFactory cspFactory, final Term operand) {
        super(cspFactory, Term.Type.NEG, operand);
    }

    @Override
    public Decomposition calculateDecomposition() {
        if (this.decompositionResult == null) {
            final Decomposition result = this.operand.decompose();
            this.decompositionResult = new Decomposition(LinearExpression.multiply(result.getLinearExpression(), -1), result.getAdditionalConstraints());
        }
        return this.decompositionResult;
    }
}
