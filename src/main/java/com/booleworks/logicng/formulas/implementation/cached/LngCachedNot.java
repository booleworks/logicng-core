// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Not;

public class LngCachedNot extends LngCachedFormula implements Not {

    private final Formula operand;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param operand the operand of the negation
     * @param f       the factory which created this instance
     */
    LngCachedNot(final Formula operand, final CachingFormulaFactory f) {
        super(FType.NOT, f);
        this.operand = operand;
        hashCode = 0;
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
            // the caching formula factory would have produced the same object
            return false;
        }
        if (other instanceof Not && hashCode() == other.hashCode()) {
            final Not otherNot = (Not) other;
            return operand.equals(otherNot.operand());
        }
        return false;
    }
}
