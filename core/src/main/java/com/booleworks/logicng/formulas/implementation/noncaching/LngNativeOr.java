// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Or;

import java.util.LinkedHashSet;

public class LngNativeOr extends LngNativeNAryOperator implements Or {

    /**
     * Constructor.
     * @param operands the list of operands
     * @param f        the factory which created this instance
     */
    LngNativeOr(final LinkedHashSet<? extends Formula> operands, final NonCachingFormulaFactory f) {
        super(FType.OR, operands, f);
    }

    /**
     * Returns {@code true} if this formula is a CNF clause, {@code false}
     * otherwise.
     * @return {@code true} if this formula is a CNF clause
     */
    @Override
    public boolean isCNFClause() {
        return operands.stream().allMatch(op -> op.getType() == FType.LITERAL);
    }

    @Override
    public int hashCode() {
        return hashCode(17);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Or && hashCode() == other.hashCode()) {
            return compareOperands(((Or) other).getOperands());
        }
        return false;
    }

}
