package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.formulas.Formula;

import java.util.ArrayList;
import java.util.List;

public final class MultiplicationFunction extends BinaryFunction {
    public MultiplicationFunction(final CspFactory cspFactory, final Term left, final Term right) {
        super(cspFactory, Term.Type.MUL, left, right);
    }

    @Override
    public Decomposition calculateDecomposition() {
        final Decomposition resultLeft = this.left.decompose();
        final Decomposition resultRight = this.right.decompose();
        final List<Formula> constraints = new ArrayList<>(resultLeft.getAdditionalConstraints());
        constraints.addAll(resultRight.getAdditionalConstraints());
        final IntegerDomain domainLeft = resultLeft.getLinearExpression().getDomain();
        final IntegerDomain domainRight = resultRight.getLinearExpression().getDomain();
        if (domainLeft.size() == 1) {
            final LinearExpression exp = LinearExpression.multiply(resultRight.getLinearExpression(), domainLeft.lb());
            return new Decomposition(exp, constraints);
        } else if (domainRight.size() == 1) {
            final LinearExpression exp = LinearExpression.multiply(resultLeft.getLinearExpression(), domainRight.lb());
            return new Decomposition(exp, constraints);
        } else if (domainLeft.size() <= domainRight.size()) {
            // left and right cannot be constants otherwise the domain size would have been 1
            final IntegerVariable a1;
            if (this.left instanceof IntegerVariable) {
                a1 = (IntegerVariable) this.left;
            } else {
                a1 = this.cspFactory.auxVariable(domainLeft);
                constraints.add(this.cspFactory.decomposeFormula(this.cspFactory.eq(a1, this.left)));
            }
            final IntegerVariable a2;
            if (this.right instanceof IntegerVariable) {
                a2 = (IntegerVariable) this.right;
            } else {
                a2 = this.cspFactory.auxVariable(domainRight);
                constraints.add(this.cspFactory.decomposeFormula(this.cspFactory.eq(a2, this.right)));
            }
            final IntegerDomain newDomain = domainLeft.mul(domainRight);
            final IntegerVariable newVariable = this.cspFactory.auxVariable(newDomain);
            constraints.add(this.cspFactory.decomposeFormula(this.cspFactory.eq(newVariable, this.cspFactory.mul(a1, a2))));
            return new Decomposition(new LinearExpression(newVariable), constraints);
        } else {
            return this.cspFactory.mul(this.right, this.left).decompose();
        }
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
