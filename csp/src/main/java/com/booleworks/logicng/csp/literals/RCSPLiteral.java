// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.literals;

/**
 * An extension to {@link ArithmeticLiteral}.
 * <p>
 * {@code RCSPLiteral}s represent simpler constraints that can efficiently
 * provide us with an upper bound.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface RCSPLiteral extends ArithmeticLiteral {
    /**
     * Returns the upper bound of this literal.
     * @return the upper bound of this literal
     */
    int getUpperBound();
}
