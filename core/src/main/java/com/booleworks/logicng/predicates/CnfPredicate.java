// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;

import java.util.Map;

/**
 * CNF predicate. Indicates whether a formula is in CNF or not.
 * @version 3.0.0
 * @since 1.0
 */
public class CnfPredicate extends CacheableFormulaPredicate {

    /**
     * Constructs a new predicate. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public CnfPredicate(final FormulaFactory f) {
        super(f, PredicateCacheEntry.IS_CNF);
    }

    /**
     * Constructs a new predicate. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public CnfPredicate(final FormulaFactory f, final Map<Formula, Boolean> cache) {
        super(f, cache);
    }

    @Override
    public boolean test(final Formula formula) {
        final Boolean cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
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
                final boolean orIsCnf = ((Or) formula).isCnfClause();
                setCache(formula, orIsCnf);
                return orIsCnf;
            case AND:
                final boolean andIsCnf = formula.stream().allMatch(this::isClause);
                setCache(formula, andIsCnf);
                return andIsCnf;
            default:
                throw new IllegalArgumentException("Cannot compute CNF predicate on " + formula.getType());
        }
    }

    protected boolean isClause(final Formula formula) {
        return formula.getType() == FType.LITERAL ||
                formula.getType() == FType.OR && ((Or) formula).isCnfClause();
    }
}
