// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;

import java.util.LinkedHashSet;

public class LngCachedOr extends LngCachedNAryOperator implements Or {

    /**
     * Constructor.
     * @param operands the list of operands
     * @param f        the factory which created this instance
     */
    LngCachedOr(final LinkedHashSet<? extends Formula> operands, final CachingFormulaFactory f) {
        super(FType.OR, operands, f);
    }

    /**
     * Returns {@code true} if this formula is a CNF clause, {@code false}
     * otherwise.
     * @return {@code true} if this formula is a CNF clause
     */
    @Override
    public boolean isCNFClause() {
        return f.getPredicateCacheForType(PredicateCacheEntry.IS_CNF).get(this) == Boolean.TRUE;
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
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            // the caching formula factory would have produced the same object
            return false;
        }
        if (other instanceof Or && hashCode() == other.hashCode()) {
            return compareOperands(((Or) other).operands());
        }
        return false;
    }

}
