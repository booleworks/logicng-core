// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;

/**
 * A transformation on a formula.
 * @version 3.0.0
 * @since 1.0
 */
@FunctionalInterface
public interface FormulaTransformation {

    /**
     * Performs the transformation with the given handler.
     * @param formula the input formula
     * @param handler the computation handler
     * @return the LNGResult with the transformed formula
     */
    LngResult<Formula> apply(Formula formula, ComputationHandler handler);

    /**
     * Performs the transformation.
     * @param formula the input formula
     * @return the transformed formula
     */
    default Formula apply(final Formula formula) {
        return apply(formula, NopHandler.get()).getResult();
    }
}
