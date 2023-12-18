// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;

/**
 * Boolean conjunction.
 * <p>
 * Invariants: - has at least two elements - does not contain duplicates - does
 * not contain complementary literals - does not contain constants
 * @version 3.0.0
 * @since 1.0
 */
public interface And extends NAryOperator {

    @Override
    default boolean evaluate(final Assignment assignment) {
        for (final Formula op : operands()) {
            if (!op.evaluate(assignment)) {
                return false;
            }
        }
        return true;
    }
}
