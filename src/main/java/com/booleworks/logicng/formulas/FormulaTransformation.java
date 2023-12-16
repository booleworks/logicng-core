// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

/**
 * A transformation on a formula.
 * @version 3.0.0
 * @since 1.0
 */
@FunctionalInterface
public interface FormulaTransformation {

    /**
     * Returns the transformed formula.
     * @param formula the input formula
     * @return the transformed formula
     */
    Formula apply(Formula formula);
}
