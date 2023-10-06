// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.cached;

import org.logicng.formulas.BinaryOperator;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;

public abstract class LngCachedBinaryOperator extends LngCachedFormula implements BinaryOperator {

    protected final Formula left;
    protected final Formula right;
    protected volatile int hashCode;

    /**
     * Constructor.
     * @param type  the type of the formula
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @param f     the factory which created this instance
     */
    LngCachedBinaryOperator(final FType type, final Formula left, final Formula right, final CachingFormulaFactory f) {
        super(type, f);
        this.left = left;
        this.right = right;
        hashCode = 0;
    }

    @Override
    public Formula left() {
        return left;
    }

    @Override
    public Formula right() {
        return right;
    }
}
