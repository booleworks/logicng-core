// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Substitution;

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
    Formula getLeft();

    /**
     * Returns the right-hand side operator.
     * @return the right-hand side operator
     */
    Formula getRight();

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
        return getLeft().containsVariable(variable) || getRight().containsVariable(variable);
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return this == formula || equals(formula) || getLeft().containsNode(formula) || getRight().containsNode(formula);
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        return f.binaryOperator(getType(), getLeft().substitute(f, substitution), getRight().substitute(f, substitution));
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
                    return getLeft();
                } else if (count == 1) {
                    count++;
                    return getRight();
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
        return Stream.of(getLeft(), getRight());
    }
}
