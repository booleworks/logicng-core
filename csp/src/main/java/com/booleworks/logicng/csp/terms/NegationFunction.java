package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.LinearExpression;

/**
 * A function term representing the negation operation.
 */
public final class NegationFunction extends UnaryFunction {

    /**
     * Constructs a new negation function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new terms.
     * @param operand the operand
     */
    public NegationFunction(final Term operand) {
        super(Term.Type.NEG, operand);
    }

    @Override
    public Decomposition calculateDecomposition(final CspFactory cf) {
        final Decomposition result = operand.decompose(cf);
        return new Decomposition(LinearExpression.multiply(result.getLinearExpression(), -1),
                result.getAdditionalConstraints(), result.getAuxiliaryIntegerVariables(),
                result.getAuxiliaryBooleanVariables());
    }
}
