// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Super class for Boolean constants.
 * @version 3.0.0
 * @since 1.0
 */
public interface Constant extends Formula {

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
    default int numberOfOperands() {
        return 0;
    }

    @Override
    default boolean isConstantFormula() {
        return true;
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
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        return this;
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return this == formula;
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        return this;
    }

    @Override
    default Iterator<Formula> iterator() {
        return ITERATOR;
    }

    @Override
    default Stream<Formula> stream() {
        return Stream.empty();
    }
}
