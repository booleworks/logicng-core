// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates.satisfiability;

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

    /**
     * Constructs a new tautology predicate with a given formula factory which caches the result.
     * @param f the formula factory
     */
    public TautologyPredicate(final FormulaFactory f) {
        this.f = f;
    }

    @Override
    public boolean test(final Formula formula) {
        return !formula.negate(f).holds(new SATPredicate(f));
    }
}
