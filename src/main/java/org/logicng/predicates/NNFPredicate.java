// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_NNF;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaPredicate;

/**
 * NNF predicate.  Indicates whether a formula is in NNF or not.
 * @version 3.0.0
 * @since 1.5.1
 */
public final class NNFPredicate implements FormulaPredicate {

    private final boolean useCache;

    public NNFPredicate() {
        this(true);
    }

    public NNFPredicate(final boolean useCache) {
        this.useCache = useCache;
    }

    @Override
    public boolean test(final Formula formula) {
        final Tristate cached = formula.predicateCacheEntry(IS_NNF);
        if (cached != Tristate.UNDEF) {
            return cached == Tristate.TRUE;
        }
        boolean result;
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
                result = true;
                break;
            case AND:
            case OR:
                result = true;
                for (final Formula op : formula) {
                    if (!test(op)) {
                        result = false;
                        break;
                    }
                }
                break;
            case NOT:
            case IMPL:
            case EQUIV:
            case PBC:
                result = false;
                break;
            default:
                throw new IllegalArgumentException("Cannot compute NNF predicate on " + formula.type());
        }
        if (useCache) {
            formula.setPredicateCacheEntry(IS_NNF, result);
        }
        return result;
    }
}
