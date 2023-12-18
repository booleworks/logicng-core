// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaPredicate;
import com.booleworks.logicng.formulas.Not;

/**
 * Predicate to test if a formula contains any subformula that is a
 * pseudo-Boolean constraint.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class ContainsPBCPredicate implements FormulaPredicate {

    private final static ContainsPBCPredicate INSTANCE = new ContainsPBCPredicate();

    /**
     * Private empty constructor. Singleton class.
     */
    private ContainsPBCPredicate() {
        // Intentionally left empty
    }

    /**
     * Returns the singleton of the predicate.
     * @return the predicate instance
     */
    public static ContainsPBCPredicate get() {
        return INSTANCE;
    }

    @Override
    public boolean test(final Formula formula) {
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
                return false;
            case AND:
            case OR:
                for (final Formula op : formula) {
                    if (test(op)) {
                        return true;
                    }
                }
                return false;
            case NOT:
                return test(((Not) formula).operand());
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                return test(binary.left()) || test(binary.right());
            case PBC:
                return true;
            default:
                throw new IllegalArgumentException("Unknown formula type " + formula.type());
        }
    }
}
