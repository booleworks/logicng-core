// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;

/**
 * Boolean constant "True".
 * @version 3.0.0
 * @since 1.0
 */
public interface CTrue extends Constant {

    @Override
    default boolean evaluate(final Assignment assignment) {
        return true;
    }

    @Override
    default Constant negate(final FormulaFactory f) {
        return f.falsum();
    }
}
