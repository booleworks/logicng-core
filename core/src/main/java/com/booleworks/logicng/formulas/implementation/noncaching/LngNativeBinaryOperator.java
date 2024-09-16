// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;

public abstract class LngNativeBinaryOperator extends LngNativeFormula implements BinaryOperator {

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
    LngNativeBinaryOperator(final FType type, final Formula left, final Formula right,
                            final NonCachingFormulaFactory f) {
        super(type, f);
        this.left = left;
        this.right = right;
        hashCode = 0;
    }

    @Override
    public Formula getLeft() {
        return left;
    }

    @Override
    public Formula getRight() {
        return right;
    }
}
