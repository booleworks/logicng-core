// SPDX-License-Identifier: Apache-2.0 and MIT
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
public interface BinaryOperator extends Formula {

    /**
     * Returns the left-hand side operator.
     * @return the left-hand side operator
     */
    Formula left();

    /**
     * Returns the right-hand side operator.
     * @return the right-hand side operator
     */
    Formula right();

    @Override
    default int numberOfOperands() {
        return 2;
    }

    @Override
    default boolean isConstantFormula() {
        return false;
    }

    @Override
    default boolean isAtomicFormula() {
        return false;
    }

    @Override
    default boolean containsVariable(final Variable variable) {
        return left().containsVariable(variable) || right().containsVariable(variable);
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return this == formula || equals(formula) || left().containsNode(formula) || right().containsNode(formula);
    }

    @Override
    default Formula substitute(final Substitution substitution, final FormulaFactory f) {
        return f.binaryOperator(type(), left().substitute(substitution, f), right().substitute(substitution, f));
    }

    @Override
    default Formula negate(final FormulaFactory f) {
        return f.not(this);
    }

    @Override
    default Iterator<Formula> iterator() {
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
                    return BinaryOperator.this.left();
                } else if (count == 1) {
                    count++;
                    return BinaryOperator.this.right();
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
    default Stream<Formula> stream() {
        return Stream.of(left(), right());
    }
}
