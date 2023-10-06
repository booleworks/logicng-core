// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.cached;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Or;
import org.logicng.formulas.cache.PredicateCacheEntry;

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
     * Returns {@code true} if this formula is a CNF clause, {@code false} otherwise.
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
            return false; // the same caching formula factory would have produced a == object
        }
        if (other instanceof Or && hashCode() == other.hashCode()) {
            return compareOperands(((Or) other).operands());
        }
        return false;
    }

}
