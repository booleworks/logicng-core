// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;

/**
 * Boolean disjunction.
 * <p>
 * Invariants:
 * <ul>
 * <li>has at least two elements</li>
 * <li>does not contain duplicates</li>
 * <li>does not contain complementary literals</li>
 * <li>does not contain constants</li>
 * </ul>
 * @version 3.0.0
 * @since 1.0
 */
public interface Or extends NAryOperator {

    @Override
    default boolean evaluate(final Assignment assignment) {
        for (final Formula op : operands()) {
            if (op.evaluate(assignment)) {
                return true;
            }
        }
        return false;
    }

    boolean isCNFClause();
}
