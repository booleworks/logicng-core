package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.LinearExpression;

import java.util.Set;
import java.util.TreeSet;

public final class MultiplicationFunction extends BinaryFunction {
    public MultiplicationFunction(final CspFactory cspFactory, final Term left, final Term right) {
        super(cspFactory, Term.Type.MUL, left, right);
    }

    @Override
    public Decomposition calculateDecomposition() {
        final Decomposition resultLeft = left.decompose();
        final Decomposition resultRight = right.decompose();
        final Set<IntegerClause> constraints = new TreeSet<>(resultLeft.getAdditionalConstraints());
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
            if (left instanceof IntegerVariable) {
                a1 = (IntegerVariable) left;
            } else {
                a1 = cspFactory.auxVariable(domainLeft);
                constraints.addAll(cspFactory.eq(a1, left).decompose());
            }
            final IntegerVariable a2;
            if (right instanceof IntegerVariable) {
                a2 = (IntegerVariable) right;
            } else {
                a2 = cspFactory.auxVariable(domainRight);
                constraints.addAll(cspFactory.eq(a2, right).decompose());
            }
            final IntegerDomain newDomain = domainLeft.mul(domainRight);
            final IntegerVariable newVariable = cspFactory.auxVariable(newDomain);
            constraints.addAll(cspFactory.eq(newVariable, cspFactory.mul(a1, a2)).decompose());
            return new Decomposition(new LinearExpression(newVariable), constraints);
        } else {
            return cspFactory.mul(right, left).decompose();
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
