// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.noncaching;

import org.logicng.formulas.CType;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.PBConstraint;

import java.util.Collection;
import java.util.List;

public class LngNativePBConstraint extends LngNativeFormula implements PBConstraint {

    protected final List<Literal> literals;
    protected final List<Integer> coefficients;
    protected CType comparator;
    protected int rhs;
    protected int maxWeight;

    /**
     * Constructs a new pseudo-Boolean constraint.
     * @param literals     the literals
     * @param coefficients the coefficients
     * @param comparator   the comparator
     * @param rhs          the right-hand side
     * @param f            the formula factory
     * @throws IllegalArgumentException if the number of literals and coefficients do not correspond
     */
    LngNativePBConstraint(final Collection<? extends Literal> literals, final Collection<Integer> coefficients, final CType comparator, final int rhs,
                          final NonCachingFormulaFactory f) {
        super(FType.PBC, f);
        if (literals.size() != coefficients.size()) {
            throw new IllegalArgumentException("Cannot generate a pseudo-Boolean constraint with literals.length != coefficients.length");
        }
        this.literals = List.copyOf(literals);
        this.coefficients = List.copyOf(coefficients);
        this.maxWeight = Integer.MIN_VALUE;
        for (final int c : coefficients) {
            if (c > this.maxWeight) {
                this.maxWeight = c;
            }
        }
        this.comparator = comparator;
        this.rhs = rhs;
    }

    @Override
    public List<Literal> operands() {
        return this.literals;
    }

    @Override
    public List<Integer> coefficients() {
        return this.coefficients;
    }

    @Override
    public CType comparator() {
        return comparator;
    }

    @Override
    public int rhs() {
        return rhs;
    }

    @Override
    public int maxWeight() {
        return maxWeight;
    }

    @Override
    public List<Formula> getEncoding() {
        return f.pbEncoder().encode(this);
    }

    @Override
    public int hashCode() {
        int hashCode = comparator.hashCode() + rhs;
        for (int i = 0; i < literals.size(); i++) {
            hashCode += 11 * literals.get(i).hashCode();
            hashCode += 13 * coefficients.get(i);
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false;
        }
        if (other instanceof PBConstraint) {
            final PBConstraint o = (PBConstraint) other;
            return rhs == o.rhs() && comparator == o.comparator()
                    && coefficients.equals(o.coefficients())
                    && literals.equals(o.operands());
        }
        return false;
    }

}
