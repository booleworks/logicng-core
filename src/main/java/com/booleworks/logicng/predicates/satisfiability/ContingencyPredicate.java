// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates.satisfiability;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaPredicate;

/**
 * Contingency predicate. Indicates whether a formula is contingent (neither a
 * tautology nor a contradiction) or not.
 * @version 3.0.0
 * @since 1.0
 */
public final class ContingencyPredicate implements FormulaPredicate {

    private final FormulaFactory f;

    /**
     * Constructs a new contingency predicate with a given formula factory which
     * caches the result.
     * @param f the formula factory
     */
    public ContingencyPredicate(final FormulaFactory f) {
        this.f = f;
    }

    @Override
    public boolean test(final Formula formula) {
        return formula.holds(new SATPredicate(f)) && !formula.holds(new TautologyPredicate(f));
    }
}
