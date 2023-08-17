// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates.satisfiability;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaPredicate;

/**
 * Contingency predicate.  Indicates whether a formula is contingent
 * (neither a tautology nor a contradiction) or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class ContingencyPredicate implements FormulaPredicate {

    private final boolean useCache;
    private final FormulaFactory f;

    /**
     * Constructs a new contingency predicate with a given formula factory which caches the result.
     * @param f the formula factory
     */
    public ContingencyPredicate(final FormulaFactory f) {
        this(f, true);
    }

    /**
     * Constructs a new contingency predicate with a given formula factory.
     * @param f        the formula factory
     * @param useCache a flag whether the result per formula should be cached
     *                 (only relevant for caching formula factory)
     */
    public ContingencyPredicate(final FormulaFactory f, final boolean useCache) {
        this.f = f;
        this.useCache = useCache;
    }

    @Override
    public boolean test(final Formula formula) {
        return formula.holds(new SATPredicate(f, useCache)) && !formula.holds(new TautologyPredicate(f, useCache));
    }
}
