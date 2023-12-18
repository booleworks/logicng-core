// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;

import java.util.Map;

/**
 * NNF predicate. Indicates whether a formula is in NNF or not.
 * @version 3.0.0
 * @since 1.5.1
 */
public final class NNFPredicate extends CacheableFormulaPredicate {

    /**
     * Constructs a new predicate. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public NNFPredicate(final FormulaFactory f) {
        super(f, PredicateCacheEntry.IS_NNF);
    }

    /**
     * Constructs a new predicate. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public NNFPredicate(final FormulaFactory f, final Map<Formula, Boolean> cache) {
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
        setCache(formula, result);
        return result;
    }
}
