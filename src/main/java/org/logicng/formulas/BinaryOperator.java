// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Substitution;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Super class for Boolean binary operators.
 * @version 3.0.0
 * @since 1.0
 */
public abstract class BinaryOperator extends LngCachedFormula {

    protected final Formula left;
    protected final Formula right;
    protected volatile int hashCode;

    /**
     * Constructor.
     * @param type  the type of the formula
     * @param left  the left-hand side operand
     * @param right the right-hand side operand
     * @param f     the factory which created this instance
     */
    BinaryOperator(final FType type, final Formula left, final Formula right, final FormulaFactory f) {
        super(type, f);
        this.left = left;
        this.right = right;
        this.hashCode = 0;
    }

    /**
     * Returns the left-hand side operator.
     * @return the left-hand side operator
     */
    public Formula left() {
        return left;
    }

    /**
     * Returns the right-hand side operator.
     * @return the right-hand side operator
     */
    public Formula right() {
        return right;
    }

    @Override
    public int numberOfOperands() {
        return 2;
    }

    @Override
    public boolean isConstantFormula() {
        return false;
    }

    @Override
    public boolean isAtomicFormula() {
        return false;
    }

    @Override
    public boolean containsVariable(final Variable variable) {
        return left.containsVariable(variable) || right.containsVariable(variable);
    }

    @Override
    public boolean containsNode(final Formula formula) {
        return this == formula || equals(formula) || left.containsNode(formula) || right.containsNode(formula);
    }

    @Override
    public Formula substitute(final Substitution substitution) {
        return f.binaryOperator(type(), left.substitute(substitution), right.substitute(substitution));
    }

    @Override
    public Formula negate() {
        return f.not(this);
    }

    @Override
    public Iterator<Formula> iterator() {
        return new Iterator<>() {
            private int count;

            @Override
            public boolean hasNext() {
                return count < 2;
            }

            @Override
            public Formula next() {
                if (count == 0) {
                    count++;
                    return BinaryOperator.this.left;
                } else if (count == 1) {
                    count++;
                    return BinaryOperator.this.right;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Stream<Formula> stream() {
        return Stream.of(left, right);
    }
}
