// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.encodings.PbEncoder;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PbConstraint;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LngCachedPbConstraint extends LngCachedFormula implements PbConstraint {

    protected final List<Literal> literals;
    protected final List<Integer> coefficients;
    protected CType comparator;
    protected int rhs;
    protected volatile int hashCode;
    protected int maxWeight;

    /**
     * Constructs a new pseudo-Boolean constraint.
     * @param literals     the literals
     * @param coefficients the coefficients
     * @param comparator   the comparator
     * @param rhs          the right-hand side
     * @param f            the formula factory
     * @throws IllegalArgumentException if the number of literals and
     *                                  coefficients do not correspond
     */
    LngCachedPbConstraint(final Collection<? extends Literal> literals, final Collection<Integer> coefficients,
                          final CType comparator, final int rhs,
                          final CachingFormulaFactory f) {
        super(FType.PBC, f);
        if (literals.size() != coefficients.size()) {
            throw new IllegalArgumentException(
                    "Cannot generate a pseudo-Boolean constraint with literals.length != coefficients.length");
        }
        this.literals = List.copyOf(literals);
        this.coefficients = List.copyOf(coefficients);
        maxWeight = Integer.MIN_VALUE;
        for (final int c : coefficients) {
            if (c > maxWeight) {
                maxWeight = c;
            }
        }
        this.comparator = comparator;
        this.rhs = rhs;
        hashCode = 0;
    }

    @Override
    public List<Literal> getOperands() {
        return literals;
    }

    @Override
    public List<Integer> getCoefficients() {
        return coefficients;
    }

    @Override
    public CType comparator() {
        return comparator;
    }

    @Override
    public int getRhs() {
        return rhs;
    }

    @Override
    public int maxWeight() {
        return maxWeight;
    }

    @Override
    public List<Formula> getEncoding(final FormulaFactory generatingFactory) {
        List<Formula> encoding;
        encoding = f.pbEncodingCache.get(this);
        if (encoding == null) {
            encoding = PbEncoder.encode(generatingFactory, this);
            if (generatingFactory == getFactory()) {
                f.pbEncodingCache.put(this, encoding);
            }
        }
        return Collections.unmodifiableList(encoding);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = computeHash();
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).getFactory()) {
            // the caching formula factory would have produced the same object
            return false;
        }
        if (other instanceof PbConstraint && hashCode() == other.hashCode()) {
            final PbConstraint o = (PbConstraint) other;
            return rhs == o.getRhs() && comparator == o.comparator() && coefficients.equals(o.getCoefficients()) &&
                    literals.equals(o.getOperands());
        }
        return false;
    }

}
