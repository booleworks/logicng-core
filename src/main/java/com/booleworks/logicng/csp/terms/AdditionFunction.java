package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.formulas.Formula;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class AdditionFunction extends NAryFunction {
    public AdditionFunction(final CspFactory cspFactory, final LinkedHashSet<Term> terms) {
        super(cspFactory, Term.Type.ADD, terms);
    }

    @Override
    public Decomposition calculateDecomposition() {
        LinearExpression.Mutable expression = new LinearExpression.Mutable(0);
        final List<Formula> constraints = new ArrayList<>();
        for (final Term operand : this.operands) {
            final Decomposition ei = operand.decompose();
            expression = expression.add(ei.getLinearExpression());
            constraints.addAll(ei.getAdditionalConstraints());
        }
        return new Decomposition(expression, constraints);
    }

}
