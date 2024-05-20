package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.LinearExpression;

public final class MultiplicationFunction extends BinaryFunction {
    public MultiplicationFunction(final IntegerConstant left, final Term right) {
        super(Term.Type.MUL, left, right);
    }

    @Override
    public Decomposition calculateDecomposition() {
        final Decomposition resultRight = right.decompose();
        final LinearExpression exp = LinearExpression.multiply(resultRight.getLinearExpression(), getLeft().getValue());
        return new Decomposition(exp, resultRight.getAdditionalConstraints());
    }

    @Override
    public IntegerConstant getLeft() {
        return (IntegerConstant) super.getLeft();
    }

    @Override
    public boolean equals(final Object o) {
        return equals(o, false);
    }

    @Override
    public int hashCode() {
        return hashCode(false);
    }
}
