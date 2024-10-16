// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;

/**
 * Boolean implication.
 * <p>
 * Invariants:
 * <ul>
 * <li>Neither operand is a constant</li>
 * <li>The operands are not the same formula</li>
 * <li>Neither operand is the negation of the other</li>
 * </ul>
 * @version 3.0.0
 * @since 1.0
 */
public interface Implication extends BinaryOperator {

    @Override
    default boolean evaluate(final Assignment assignment) {
        return !getLeft().evaluate(assignment) || getRight().evaluate(assignment);
    }

    @Override
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        return f.implication(getLeft().restrict(f, assignment), getRight().restrict(f, assignment));
    }
}
