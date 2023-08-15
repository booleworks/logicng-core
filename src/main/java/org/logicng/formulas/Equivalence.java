// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Assignment;

/**
 * Boolean equivalence.
 * @version 3.0.0
 * @since 1.0
 */
public interface Equivalence extends BinaryOperator {

    @Override
    default boolean evaluate(final Assignment assignment) {
        return left().evaluate(assignment) == right().evaluate(assignment);
    }

    @Override
    default Formula restrict(final Assignment assignment, final FormulaFactory f) {
        return f.equivalence(left().restrict(assignment), right().restrict(assignment));
    }
}
