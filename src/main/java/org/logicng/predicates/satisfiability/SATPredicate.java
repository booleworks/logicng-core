// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates.satisfiability;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_SAT;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaPredicate;
import org.logicng.solvers.MiniSat;

/**
 * A SAT solver based SAT predicate.  Indicates whether a formula is satisfiable or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class SATPredicate implements FormulaPredicate {

    private final boolean useCache;
    private final FormulaFactory f;

    /**
     * Constructs a new SAT predicate with a given formula factory which caches its result.
     * @param f the formula factory
     */
    public SATPredicate(final FormulaFactory f) {
        this(f, true);
    }

    /**
     * Constructs a new SAT predicate with a given formula factory.
     * @param f        the formula factory
     * @param useCache a flag whether the result per formula should be cached
     *                 (only relevant for caching formula factory)
     */
    public SATPredicate(final FormulaFactory f, final boolean useCache) {
        this.f = f;
        this.useCache = useCache;
    }

    @Override
    public boolean test(final Formula formula) {
        final Tristate cached = formula.predicateCacheEntry(IS_SAT);
        if (cached != Tristate.UNDEF) {
            return cached == Tristate.TRUE;
        }
        final boolean result;
        if (formula.type() == FType.FALSE) {
            result = false;
        } else {
            final var solver = MiniSat.miniSat(f);
            solver.add(formula);
            result = solver.sat() == Tristate.TRUE;
        }
        if (useCache) {
            formula.setPredicateCacheEntry(IS_SAT, result);
        }
        return result;
    }
}
