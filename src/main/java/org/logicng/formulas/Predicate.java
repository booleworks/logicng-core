package org.logicng.formulas;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

interface Predicate extends Formula {
    Iterator<Formula> ITERATOR = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Formula next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    @Override
    long numberOfNodes();

    @Override
    long numberOfInternalNodes();

    @Override
    default int numberOfOperands() {
        return 0;
    }

    @Override
    default boolean isConstantFormula() {
        return false;
    }

    @Override
    default boolean isAtomicFormula() {
        return true;
    }

    @Override
    default boolean containsVariable(final Variable variable) {
        return false;
    }

    @Override
    default boolean evaluate(final Assignment assignment) {
        throw new UnsupportedOperationException("Cannot evaluate a formula with predicates with a boolean assignment");
    }

    @Override
    default Formula restrict(final Assignment assignment) {
        return this;
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return this.equals(formula);
    }

    @Override
    default Formula substitute(final Substitution substitution) {
        return this;
    }

    @Override
    default Formula negate() {
        return this.factory().not(this);
    }

    @Override
    default Stream<Formula> stream() {
        return Stream.empty();
    }

    @Override
    default Iterator<Formula> iterator() {
        return ITERATOR;
    }
}
