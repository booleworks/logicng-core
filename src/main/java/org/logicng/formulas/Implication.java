// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Assignment;

/**
 * Boolean implication.
 * @version 3.0.0
 * @since 1.0
 */
public final class Implication extends BinaryOperator {

    /**
     * Constructor.
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @param f     the factory which created this instance
     */
    Implication(final Formula left, final Formula right, final FormulaFactory f) {
        super(FType.IMPL, left, right, f);
    }

    @Override
    public boolean evaluate(final Assignment assignment) {
        return !left.evaluate(assignment) || right.evaluate(assignment);
    }

    @Override
    public Formula restrict(final Assignment assignment) {
        return f.implication(left.restrict(assignment), right.restrict(assignment));
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
            return false; // the same formula factory would have produced a == object
        }
        if (other instanceof Implication) {
            final Implication otherImp = (Implication) other;
            return left.equals(otherImp.left) && right.equals(otherImp.right);
        }
        return false;
    }
}
