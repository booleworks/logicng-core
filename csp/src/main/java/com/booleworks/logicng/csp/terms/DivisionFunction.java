// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.predicates.CspPredicate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A function term representing the division operation.
 * @version 3.0.0
 * @since 3.0.0
 */
public class DivisionFunction extends BinaryFunction {
    /**
     * Prefix for auxiliary variables introduced by the decomposition.
     */
    public final static String DIV_AUX_VARIABLE = "DIV";

    /**
     * Constructs a new division function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to
     * create new terms.
     * @param left  the dividend
     * @param right the divisor
     */
    public DivisionFunction(final Term left, final IntegerConstant right) {
        super(Term.Type.DIV, left, right);
    }

    @Override
    public Term restrict(final CspFactory cf, final CspAssignment restrictions) {
        return cf.div(getLeft().restrict(cf, restrictions), getRight().restrict(cf, restrictions));
    }

    @Override
    protected Decomposition calculateDecomposition(final CspFactory cf) {
        final Decomposition resultLeft = left.decompose(cf);
        final Set<IntegerVariable> intVars = new LinkedHashSet<>(resultLeft.getAuxiliaryIntegerVariables());
        final IntegerDomain domainLeft = resultLeft.getLinearExpression().getDomain();
        final int rightValue = getRight().getValue();
        final IntegerVariable q = cf.auxVariable(DIV_AUX_VARIABLE, domainLeft.div(rightValue));
        final IntegerVariable r = cf.auxVariable(DIV_AUX_VARIABLE, domainLeft.mod(rightValue));
        intVars.add(q);
        intVars.add(r);
        final Term px = cf.mul(getRight(), q);
        final CspPredicate.Decomposition d1 = cf.eq(left, cf.add(px, r)).decompose(cf);
        final CspPredicate.Decomposition d2 = cf.ge(r, cf.zero()).decompose(cf);
        final CspPredicate.Decomposition d3 = cf.gt(cf.constant(Math.abs(rightValue)), r).decompose(cf);
        final Term.Decomposition newTerm =
                new Term.Decomposition(new LinearExpression(q), resultLeft.getAdditionalConstraints(), intVars,
                        resultLeft.getAuxiliaryBooleanVariables());
        return Term.Decomposition.merge(newTerm, List.of(d1, d2, d3));
    }

    @Override
    public IntegerConstant getRight() {
        return (IntegerConstant) super.getRight();
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
