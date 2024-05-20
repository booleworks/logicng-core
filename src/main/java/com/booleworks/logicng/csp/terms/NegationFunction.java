package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.LinearExpression;

public final class NegationFunction extends UnaryFunction {

    public NegationFunction(final Term operand) {
        super(Term.Type.NEG, operand);
    }

    @Override
    public Decomposition calculateDecomposition() {
        final Decomposition result = operand.decompose();
        return new Decomposition(LinearExpression.multiply(result.getLinearExpression(), -1), result.getAdditionalConstraints());
    }
}
