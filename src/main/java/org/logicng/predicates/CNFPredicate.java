// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.logicng.formulas.cache.PredicateCacheEntry.IS_CNF;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaPredicate;
import org.logicng.formulas.Or;

/**
 * CNF predicate.  Indicates whether a formula is in CNF or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class CNFPredicate implements FormulaPredicate {

    private final static CNFPredicate INSTANCE = new CNFPredicate();

    /**
     * Private empty constructor.  Singleton class.
     */
    private CNFPredicate() {
        // Intentionally left empty
    }

    /**
     * Returns the singleton of the predicate.
     * @return the predicate instance
     */
    public static CNFPredicate get() {
        return INSTANCE;
    }

    @Override
    public boolean test(final Formula formula, final boolean cache) {
        final Tristate cached = formula.predicateCacheEntry(IS_CNF);
        if (cached != Tristate.UNDEF) {
            return cached == Tristate.TRUE;
        }
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
                return ((Or) formula).isCNFClause();
            case AND:
                return formula.stream().allMatch(this::isClause);
            default:
                throw new IllegalArgumentException("Cannot compute CNF predicate on " + formula.type());
        }
    }

    private boolean isClause(final Formula formula) {
        return formula.type() == FType.LITERAL ||
                formula.type() == FType.OR && ((Or) formula).isCNFClause();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
