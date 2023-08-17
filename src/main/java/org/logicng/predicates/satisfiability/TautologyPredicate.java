// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates.satisfiability;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_TAUTOLOGY;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaPredicate;

/**
 * Tautology predicate.  Indicates whether a formula is a tautology or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class TautologyPredicate implements FormulaPredicate {

    private final FormulaFactory f;
    private final boolean useCache;

    /**
     * Constructs a new tautology predicate with a given formula factory which caches the result.
     * @param f the formula factory
     */
    public TautologyPredicate(final FormulaFactory f) {
        this(f, true);
    }

    /**
     * Constructs a new tautology predicate with a given formula factory.
     * @param f        the formula factory
     * @param useCache a flag whether the result per formula should be cached
     *                 (only relevant for caching formula factory)
     */
    public TautologyPredicate(final FormulaFactory f, final boolean useCache) {
        this.f = f;
        this.useCache = useCache;
    }

    @Override
    public boolean test(final Formula formula) {
        final Tristate cached = formula.predicateCacheEntry(IS_TAUTOLOGY);
        if (cached != Tristate.UNDEF) {
            return cached == Tristate.TRUE;
        }
        final boolean result;
        final Formula negation = formula.negate(f);
        result = !negation.holds(new SATPredicate(f, useCache));
        if (useCache) {
            formula.setPredicateCacheEntry(IS_TAUTOLOGY, result);
        }
        return result;
    }
}
