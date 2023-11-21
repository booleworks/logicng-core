// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.NAryOperator;

import java.util.Collection;
import java.util.List;

public abstract class LngNativeNAryOperator extends LngNativeFormula implements NAryOperator {

    protected final List<Formula> operands;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param type     the operator's type
     * @param operands the list of operands
     * @param f        the factory which created this instance
     */
    LngNativeNAryOperator(final FType type, final Collection<? extends Formula> operands, final NonCachingFormulaFactory f) {
        super(type, f);
        this.operands = List.copyOf(operands);
        hashCode = 0;
    }

    @Override
    public List<Formula> operands() {
        return operands;
    }

    /**
     * Helper method for generating the hashcode.
     * @param shift shift value
     * @return hashcode
     */
    protected int hashCode(final int shift) {
        if (hashCode == 0) {
            hashCode = computeHash(shift);
        }
        return hashCode;
    }
}
