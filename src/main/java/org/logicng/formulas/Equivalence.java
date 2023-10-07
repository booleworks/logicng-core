// SPDX-License-Identifier: Apache-2.0 and MIT
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
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        return f.equivalence(left().restrict(f, assignment), right().restrict(f, assignment));
    }
}
