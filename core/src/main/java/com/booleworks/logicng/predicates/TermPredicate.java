// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaPredicate;
import com.booleworks.logicng.formulas.NAryOperator;

/**
 * Term predicate. Indicates whether a formula is a minterm (conjunction of
 * literals) or maxterm (disjunction of literals).
 * @version 3.0.0
 * @since 2.2.0
 */
public final class TermPredicate implements FormulaPredicate {

    private final static TermPredicate MINTERM_PREDICATE = new TermPredicate(true);
    private final static TermPredicate MAXTERM_PREDICATE = new TermPredicate(false);

    private final boolean mintermPredicate;

    /**
     * Private empty constructor. Singleton class.
     */
    private TermPredicate(final boolean mintermPredicate) {
        this.mintermPredicate = mintermPredicate;
    }

    /**
     * Returns the singleton minterm predicate.
     * @return the minterm predicate instance
     */
    public static TermPredicate minterm() {
        return MINTERM_PREDICATE;
    }

    /**
     * Returns the singleton maxterm predicate.
     * @return the maxterm predicate instance
     */
    public static TermPredicate maxterm() {
        return MAXTERM_PREDICATE;
    }

    @Override
    public boolean test(final Formula formula) {
        switch (formula.getType()) {
            case TRUE:
            case FALSE:
            case LITERAL:
            case PREDICATE:
                return true;
            case IMPL:
            case EQUIV:
            case PBC:
            case NOT:
                return false;
            case OR:
                if (mintermPredicate) {
                    return false;
                }
                return onlyLiteralOperands((NAryOperator) formula);
            case AND:
                if (!mintermPredicate) {
                    return false;
                }
                return onlyLiteralOperands((NAryOperator) formula);
            default:
                throw new IllegalArgumentException("Unknown formula type: " + formula.getType());
        }
    }

    private boolean onlyLiteralOperands(final NAryOperator nary) {
        for (final Formula op : nary) {
            if (op.getType() != FType.LITERAL) {
                return false;
            }
        }
        return true;
    }
}

