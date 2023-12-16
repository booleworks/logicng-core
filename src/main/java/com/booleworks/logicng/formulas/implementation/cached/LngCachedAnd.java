// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;

import java.util.LinkedHashSet;

public class LngCachedAnd extends LngCachedNAryOperator implements And {

    /**
     * Constructor.
     * @param operands the stream of operands
     * @param f        the factory which created this instance
     */
    LngCachedAnd(final LinkedHashSet<? extends Formula> operands, final CachingFormulaFactory f) {
        super(FType.AND, operands, f);
    }

    @Override
    public int hashCode() {
        return hashCode(31);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false; // the same caching formula factory would have produced a == object
        }
        if (other instanceof And && hashCode() == other.hashCode()) {
            return compareOperands(((And) other).operands());
        }
        return false;
    }
}
