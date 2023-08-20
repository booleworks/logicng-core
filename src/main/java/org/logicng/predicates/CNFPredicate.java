// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_CNF;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Or;

import java.util.Map;

/**
 * CNF predicate.  Indicates whether a formula is in CNF or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class CNFPredicate extends CacheableFormulaPredicate {

    /**
     * Constructs a new predicate.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public CNFPredicate(final FormulaFactory f) {
        super(f, IS_CNF);
    }

    /**
     * Constructs a new predicate.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public CNFPredicate(final FormulaFactory f, final Map<Formula, Boolean> cache) {
        super(f, cache);
    }

    @Override
    public boolean test(final Formula formula) {
        final Boolean cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
                return true;
            case NOT:
            case IMPL:
            case EQUIV:
            case PBC:
                return false;
            case OR:
                final boolean orIsCnf = ((Or) formula).isCNFClause();
                setCache(formula, orIsCnf);
                return orIsCnf;
            case AND:
                final boolean andIsCnf = formula.stream().allMatch(this::isClause);
                setCache(formula, andIsCnf);
                return andIsCnf;
            default:
                throw new IllegalArgumentException("Cannot compute CNF predicate on " + formula.type());
        }
    }

    private boolean isClause(final Formula formula) {
        return formula.type() == FType.LITERAL ||
                formula.type() == FType.OR && ((Or) formula).isCNFClause();
    }
}
