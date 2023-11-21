// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.printer.DefaultStringRepresentation;

/**
 * A rating function which rates a formula with the length of its representation
 * using the {@link DefaultStringRepresentation}.
 * @version 3.0.0
 * @since 2.0.0
 */
public class DefaultRatingFunction implements RatingFunction<Integer> {

    private final DefaultStringRepresentation formatter = new DefaultStringRepresentation();
    private static final DefaultRatingFunction INSTANCE = new DefaultRatingFunction();

    private DefaultRatingFunction() {
        // Intentionally left empty
    }

    /**
     * Returns the singleton instance of this function.
     * @return an instance of this function
     */
    public static DefaultRatingFunction get() {
        return INSTANCE;
    }

    @Override
    public Integer apply(final Formula formula) {
        return formatter.toString(formula).length();
    }
}
