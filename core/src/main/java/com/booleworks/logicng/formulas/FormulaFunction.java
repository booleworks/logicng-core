// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;

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
     * @param handler the computation handler
     * @return the result of the application or the canceling cause if the
     * handler cancelled the computation.
     */
    LngResult<T> apply(Formula formula, ComputationHandler handler);

    /**
     * Applies this function to a given formula.
     * @param formula the input formula
     * @return the result of the application
     */
    default T apply(final Formula formula) {
        return apply(formula, NopHandler.get()).getResult();
    }
}
