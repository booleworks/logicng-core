// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Not;

public class LngNativeNot extends LngNativeFormula implements Not {

    private final Formula operand;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param operand the operand of the negation
     * @param f       the factory which created this instance
     */
    LngNativeNot(final Formula operand, final NonCachingFormulaFactory f) {
        super(FType.NOT, f);
        this.operand = operand;
        hashCode = 0;
    }

    @Override
    public Formula getOperand() {
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
        if (other instanceof Not && hashCode() == other.hashCode()) {
            final Not otherNot = (Not) other;
            return operand.equals(otherNot.getOperand());
        }
        return false;
    }
}
