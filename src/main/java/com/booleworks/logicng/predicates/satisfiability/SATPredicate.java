// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates.satisfiability;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;
import com.booleworks.logicng.predicates.CacheableFormulaPredicate;
import com.booleworks.logicng.solvers.MiniSat;

import java.util.Map;

/**
 * A SAT solver based SAT predicate.  Indicates whether a formula is satisfiable or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class SATPredicate extends CacheableFormulaPredicate {

    /**
     * Constructs a new predicate.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public SATPredicate(final FormulaFactory f) {
        super(f, PredicateCacheEntry.IS_SAT);
    }

    /**
     * Constructs a new predicate.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public SATPredicate(final FormulaFactory f, final Map<Formula, Boolean> cache) {
        super(f, cache);
    }

    @Override
    public boolean test(final Formula formula) {
        final Boolean cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final boolean result;
        if (formula.type() == FType.FALSE) {
            result = false;
        } else {
            final var solver = MiniSat.miniSat(f);
            solver.add(formula);
            result = solver.sat() == Tristate.TRUE;
        }
        setCache(formula, result);
        return result;
    }
}
