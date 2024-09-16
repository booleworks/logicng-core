// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;

public class LngNativeEquivalence extends LngNativeBinaryOperator implements Equivalence {

    /**
     * Private constructor (initialize with {@code createNAryOperator()})
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @param f     the factory which created this instance
     */
    LngNativeEquivalence(final Formula left, final Formula right, final NonCachingFormulaFactory f) {
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
        if (other instanceof Equivalence && hashCode() == other.hashCode()) {
            final Equivalence otherEq = (Equivalence) other;
            return left.equals(otherEq.getLeft()) && right.equals(otherEq.getRight()) ||
                    left.equals(otherEq.getRight()) && right.equals(otherEq.getLeft());
        }
        return false;
    }
}
