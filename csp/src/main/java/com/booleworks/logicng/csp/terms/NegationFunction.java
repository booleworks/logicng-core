// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.LinearExpression;

/**
 * A function term representing the negation operation.
 * @version 3.0.0
 * @since 3.0.0
 */
public class NegationFunction extends UnaryFunction {

    /**
     * Constructs a new negation function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to
     * create new terms.
     * @param operand the operand
     */
    public NegationFunction(final Term operand) {
        super(Term.Type.NEG, operand);
    }

    @Override
    public Term restrict(final CspFactory cf, final CspAssignment restrictions) {
        return cf.minus(getOperand().restrict(cf, restrictions));
    }

    @Override
    public Decomposition calculateDecomposition(final CspFactory cf) {
        final Decomposition result = operand.decompose(cf);
        return new Decomposition(LinearExpression.multiply(result.getLinearExpression(), -1),
                result.getAdditionalConstraints(), result.getAuxiliaryIntegerVariables(),
                result.getAuxiliaryBooleanVariables());
    }
}
