// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

/**
 * A function on a formula.
 * @param <T> the result type of the function
 * @version 3.0.0
 * @since 1.0
 */
@FunctionalInterface
public interface FormulaFunction<T> {

    /**
     * Applies this function to a given formula.
     * @param formula the input formula
     * @return the result of the application
     */
    T apply(Formula formula);
}
