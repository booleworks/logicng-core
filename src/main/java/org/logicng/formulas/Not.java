// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Boolean negation.
 * @version 3.0.0
 * @since 1.0
 */
public interface Not extends Formula {

    /**
     * Returns the operand of this negation.
     * @return the operand of this negation
     */
    Formula operand();

    @Override
    default int numberOfOperands() {
        return 1;
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
        return operand().containsVariable(variable);
    }

    @Override
    default boolean evaluate(final Assignment assignment) {
        return !operand().evaluate(assignment);
    }

    @Override
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        return f.not(operand().restrict(f, assignment));
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return this == formula || equals(formula) || operand().containsNode(formula);
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        return f.not(operand().substitute(f, substitution));
    }

    @Override
    default Formula negate(final FormulaFactory f) {
        return operand();
    }

    @Override
    default Iterator<Formula> iterator() {
        return new Iterator<>() {
            private boolean iterated;

            @Override
            public boolean hasNext() {
                return !iterated;
            }

            @Override
            public Formula next() {
                if (!iterated) {
                    iterated = true;
                    return operand();
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
        return Stream.of(operand());
    }
}
