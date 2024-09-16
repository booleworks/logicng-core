// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import static com.booleworks.logicng.predicates.TermPredicate.minterm;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;

import java.util.Map;

/**
 * DNF predicate. Indicates whether a formula is in DNF or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class DNFPredicate extends CacheableFormulaPredicate {

    /**
     * Constructs a new predicate. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public DNFPredicate(final FormulaFactory f) {
        super(f, PredicateCacheEntry.IS_DNF);
    }

    /**
     * Constructs a new predicate. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public DNFPredicate(final FormulaFactory f, final Map<Formula, Boolean> cache) {
        super(f, cache);
    }

    @Override
    public boolean test(final Formula formula) {
        final Boolean cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        boolean result;
        switch (formula.getType()) {
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
                throw new IllegalArgumentException("Cannot compute DNF predicate on " + formula.getType());
        }
        setCache(formula, result);
        return result;
    }
}
