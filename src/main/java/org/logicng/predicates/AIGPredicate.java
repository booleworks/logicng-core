// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_AIG;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaPredicate;
import org.logicng.formulas.Not;

/**
 * And-inverter-graph (AIG) predicate.  Returns {@code true} if the given formula is an AIG, {@code false} otherwise.
 * @version 3.0.0
 * @since 1.0
 */
public final class AIGPredicate implements FormulaPredicate {
    private final boolean useCache;

    public AIGPredicate() {
        this(true);
    }

    public AIGPredicate(final boolean useCache) {
        this.useCache = useCache;
    }

    @Override
    public boolean test(final Formula formula) {
        final Tristate cached = formula.predicateCacheEntry(IS_AIG);
        if (cached != Tristate.UNDEF) {
            return cached == Tristate.TRUE;
        }
        boolean result;
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
                result = true;
                break;
            case IMPL:
            case EQUIV:
            case OR:
            case PBC:
            case PREDICATE:
                result = false;
                break;
            case NOT:
                result = test(((Not) formula).operand());
                break;
            case AND:
                result = true;
                for (final Formula op : formula) {
                    if (!test(op)) {
                        result = false;
                        break;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot compute AIG predicate on " + formula.type());
        }
        if (useCache) {
            formula.setPredicateCacheEntry(IS_AIG, result);
        }
        return result;
    }
}
