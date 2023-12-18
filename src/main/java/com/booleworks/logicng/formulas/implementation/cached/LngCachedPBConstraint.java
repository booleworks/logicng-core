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
import com.booleworks.logicng.formulas.PBConstraint;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LngCachedPBConstraint extends LngCachedFormula implements PBConstraint {

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
    LngCachedPBConstraint(final Collection<? extends Literal> literals, final Collection<Integer> coefficients,
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
    public List<Literal> operands() {
        return literals;
    }

    @Override
    public List<Integer> coefficients() {
        return coefficients;
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
    public List<Formula> getEncoding(final FormulaFactory generatingFactory) {
        List<Formula> encoding;
        encoding = f.pbEncodingCache.get(this);
        if (encoding == null) {
            encoding = PbEncoder.encode(generatingFactory, this);
            if (generatingFactory == factory()) {
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
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false; // the same caching formula factory would have
            // produced a == object
        }
        if (other instanceof PBConstraint && hashCode() == other.hashCode()) {
            final PBConstraint o = (PBConstraint) other;
            return rhs == o.rhs() && comparator == o.comparator() && coefficients.equals(o.coefficients()) &&
                    literals.equals(o.operands());
        }
        return false;
    }

}
