// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_AIG;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Not;

import java.util.Map;

/**
 * And-inverter-graph (AIG) predicate.  Returns {@code true} if the given formula is an AIG, {@code false} otherwise.
 * @version 3.0.0
 * @since 1.0
 */
public final class AIGPredicate extends CacheableFormulaPredicate {

    /**
     * Constructs a new predicate.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public AIGPredicate(final FormulaFactory f) {
        super(f, IS_AIG);
    }

    /**
     * Constructs a new predicate.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public AIGPredicate(final FormulaFactory f, final Map<Formula, Boolean> cache) {
        super(f, cache);
    }

    @Override
    public boolean test(final Formula formula) {
        final Boolean cached = lookupCache(formula);
        if (cached != null) {
            return cached;
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
        setCache(formula, result);
        return result;
    }
}
