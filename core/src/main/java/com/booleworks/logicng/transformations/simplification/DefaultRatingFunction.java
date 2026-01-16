// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.printer.DefaultStringRepresentation;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

/**
 * A rating function which rates a formula with the length of its representation
 * using the {@link DefaultStringRepresentation}.
 * @version 3.0.0
 * @since 2.0.0
 */
public class DefaultRatingFunction implements RatingFunction<Integer> {

    private final DefaultStringRepresentation formatter = DefaultStringRepresentation.get();

    /**
     * Returns a new rating function which rates a formula by the length of its
     * {@link DefaultStringRepresentation}.
     */
    public DefaultRatingFunction() {
        // Intentionally left empty
    }

    @Override
    public LngResult<Integer> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(formatter.toString(formula).length());
    }
}
