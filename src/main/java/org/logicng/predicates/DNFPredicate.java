// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_DNF;
import static org.logicng.predicates.TermPredicate.minterm;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaPredicate;

/**
 * DNF predicate.  Indicates whether a formula is in DNF or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class DNFPredicate implements FormulaPredicate {

    private final boolean useCache;

    public DNFPredicate() {
        this(true);
    }

    public DNFPredicate(final boolean useCache) {
        this.useCache = useCache;
    }

    @Override
    public boolean test(final Formula formula) {
        final Tristate cached = formula.predicateCacheEntry(IS_DNF);
        if (cached != Tristate.UNDEF) {
            return cached == Tristate.TRUE;
        }
        boolean result;
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
                return true;
            case NOT:
            case IMPL:
            case EQUIV:
            case PBC:
                return false;
            case OR:
                result = true;
                for (final Formula op : formula) {
                    if (!minterm().test(op)) {
                        result = false;
                    }
                }
                break;
            case AND:
                result = minterm().test(formula);
                break;
            default:
                throw new IllegalArgumentException("Cannot compute DNF predicate on " + formula.type());
        }
        if (useCache) {
            formula.setPredicateCacheEntry(IS_DNF, result);
        }
        return result;
    }
}
