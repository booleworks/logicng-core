// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Implication;

public class LngCachedImplication extends LngCachedBinaryOperator implements Implication {

    /**
     * Constructor.
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @param f     the factory which created this instance
     */
    LngCachedImplication(final Formula left, final Formula right, final CachingFormulaFactory f) {
        super(FType.IMPL, left, right, f);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 37 * left.hashCode() + 39 * right.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            // the caching formula factory would have produced the same object
            return false;
        }
        if (other instanceof Implication && hashCode() == other.hashCode()) {
            final Implication otherImp = (Implication) other;
            return left.equals(otherImp.left()) && right.equals(otherImp.right());
        }
        return false;
    }
}
