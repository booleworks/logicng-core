// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public interface Predicate extends Formula {
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
    default FType getType() {
        return FType.PREDICATE;
    }

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
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        return this;
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return equals(formula);
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        return this;
    }

    @Override
    default Formula negate(final FormulaFactory f) {
        return f.not(this);
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
