// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

public class LngCachedNot extends LngCachedFormula implements Not {

    private final Formula operand;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param operand the operand of the negation
     * @param f       the factory which created this instance
     */
    LngCachedNot(final Formula operand, final FormulaFactory f) {
        super(FType.NOT, f);
        this.operand = operand;
        this.hashCode = 0;
    }

    @Override
    public Formula operand() {
        return operand;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 29 * operand.hashCode();
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
        if (other instanceof Not) {
            final Not otherNot = (Not) other;
            return operand.equals(otherNot.operand());
        }
        return false;
    }
}
