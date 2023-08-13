// SPDX-License-Identifier: Apache-2.0
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
public final class Not extends LngCachedFormula {

    private final Formula operand;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param operand the operand of the negation
     * @param f       the factory which created this instance
     */
    Not(final Formula operand, final FormulaFactory f) {
        super(FType.NOT, f);
        this.operand = operand;
        this.hashCode = 0;
    }

    /**
     * Returns the operand of this negation.
     * @return the operand of this negation
     */
    public Formula operand() {
        return operand;
    }

    @Override
    public int numberOfOperands() {
        return 1;
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
        return operand.containsVariable(variable);
    }

    @Override
    public boolean evaluate(final Assignment assignment) {
        return !operand.evaluate(assignment);
    }

    @Override
    public Formula restrict(final Assignment assignment) {
        return f.not(operand.restrict(assignment));
    }

    @Override
    public boolean containsNode(final Formula formula) {
        return this == formula || equals(formula) || operand.containsNode(formula);
    }

    @Override
    public Formula substitute(final Substitution substitution) {
        return f.not(operand.substitute(substitution));
    }

    @Override
    public Formula negate() {
        return operand;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 29 * operand.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false; // the same formula factory would have produced a == object
        }
        if (other instanceof Not) {
            final Not otherNot = (Not) other;
            return operand.equals(otherNot.operand);
        }
        return false;
    }

    @Override
    public Iterator<Formula> iterator() {
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
                    return Not.this.operand;
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
        return Stream.of(operand);
    }
}
