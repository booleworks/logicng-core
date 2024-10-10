package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.formulas.Variable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A function term representing the addition operation.
 */
public final class AdditionFunction extends NAryFunction {
    /**
     * Constructs a new addition function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new terms.
     * @param terms the operands
     */
    public AdditionFunction(final LinkedHashSet<Term> terms) {
        super(Term.Type.ADD, terms);
    }

    @Override
    public Decomposition calculateDecomposition(final CspFactory cf) {
        LinearExpression.Builder expression = new LinearExpression.Builder(0);
        final Set<IntegerClause> constraints = new LinkedHashSet<>();
        final Set<IntegerVariable> auxIntVars = new LinkedHashSet<>();
        final Set<Variable> auxBoolVars = new LinkedHashSet<>();
        for (final Term operand : operands) {
            final Decomposition ei = operand.decompose(cf);
            expression = expression.add(ei.getLinearExpression());
            constraints.addAll(ei.getAdditionalConstraints());
            auxIntVars.addAll(ei.getAuxiliaryIntegerVariables());
            auxBoolVars.addAll(ei.getAuxiliaryBooleanVariables());
        }
        return new Decomposition(expression.build(), constraints, auxIntVars, auxBoolVars);
    }

}
