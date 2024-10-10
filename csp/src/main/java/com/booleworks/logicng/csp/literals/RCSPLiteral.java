package com.booleworks.logicng.csp.literals;

/**
 * An extension to {@link ArithmeticLiteral}.
 * <p>
 * {@code RCSPLiteral}s represent simpler constraints that can efficiently provide us with an upper bound.
 */
public interface RCSPLiteral extends ArithmeticLiteral {
    /**
     * Returns the upper bound of this literal.
     * @return the upper bound of this literal
     */
    int getUpperBound();
}
