// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.cached;

import org.logicng.formulas.Equivalence;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;

public class LngCachedEquivalence extends LngCachedBinaryOperator implements Equivalence {

    /**
     * Private constructor (initialize with {@code createNAryOperator()})
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @param f     the factory which created this instance
     */
    LngCachedEquivalence(final Formula left, final Formula right, final CachingFormulaFactory f) {
        super(FType.EQUIV, left, right, f);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 41 * (left.hashCode() + right.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false; // the same caching formula factory would have produced a == object
        }
        if (other instanceof Equivalence && hashCode() == other.hashCode()) {
            final Equivalence otherEq = (Equivalence) other;
            return left.equals(otherEq.left()) && right.equals(otherEq.right()) ||
                    left.equals(otherEq.right()) && right.equals(otherEq.left());
        }
        return false;
    }
}
