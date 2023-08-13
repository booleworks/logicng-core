// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import java.util.Collection;
import java.util.List;

public abstract class LngCachedNAryOperator extends LngCachedFormula implements NAryOperator {

    protected final List<Formula> operands;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param type     the operator's type
     * @param operands the list of operands
     * @param f        the factory which created this instance
     */
    LngCachedNAryOperator(final FType type, final Collection<? extends Formula> operands, final FormulaFactory f) {
        super(type, f);
        this.operands = List.copyOf(operands);
        this.hashCode = 0;
    }

    @Override
    public List<Formula> operands() {
        return this.operands;
    }

    /**
     * Helper method for generating the hashcode.
     * @param shift shift value
     * @return hashcode
     */
    protected int hashCode(final int shift) {
        if (hashCode == 0) {
            int temp = 1;
            for (final Formula formula : operands) {
                temp += formula.hashCode();
            }
            temp *= shift;
            hashCode = temp;
        }
        return hashCode;
    }
}
