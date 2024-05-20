package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.LinearExpression;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public final class AdditionFunction extends NAryFunction {
    public AdditionFunction(final LinkedHashSet<Term> terms) {
        super(Term.Type.ADD, terms);
    }

    @Override
    public Decomposition calculateDecomposition() {
        LinearExpression.Builder expression = new LinearExpression.Builder(0);
        final Set<IntegerClause> constraints = new TreeSet<>();
        for (final Term operand : operands) {
            final Decomposition ei = operand.decompose();
            expression = expression.add(ei.getLinearExpression());
            constraints.addAll(ei.getAdditionalConstraints());
        }
        return new Decomposition(expression.build(), constraints);
    }

}
