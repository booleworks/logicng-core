// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Assignment;

/**
 * Boolean constant "False".
 * @version 3.0.0
 * @since 1.0
 */
public interface CFalse extends Constant {

    @Override
    default boolean evaluate(final Assignment assignment) {
        return false;
    }

    @Override
    default Constant negate(final FormulaFactory f) {
        return f.verum();
    }
}
