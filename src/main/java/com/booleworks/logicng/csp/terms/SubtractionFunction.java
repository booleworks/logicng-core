package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.formulas.Formula;

import java.util.ArrayList;
import java.util.List;

public final class SubtractionFunction extends BinaryFunction {
    public SubtractionFunction(final CspFactory cspFactory, final Term left, final Term right) {
        super(cspFactory, Term.Type.SUB, left, right);
    }

    @Override
    public Decomposition calculateDecomposition() {
        final Decomposition resultLeft = this.left.decompose();
        LinearExpression expression = resultLeft.getLinearExpression();
        final List<Formula> constraints = new ArrayList<>(resultLeft.getAdditionalConstraints());
        final Decomposition resultRight = this.right.decompose();
        expression = LinearExpression.add(expression, LinearExpression.multiply(resultRight.getLinearExpression(), -1));
        constraints.addAll(resultRight.getAdditionalConstraints());
        return new Decomposition(expression, constraints);
    }

    @Override
    public boolean equals(final Object o) {
        return equals(o, true);
    }

    @Override
    public int hashCode() {
        return hashCode(true);
    }
}
