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
 * Boolean literals.
 * <p>
 * Literals are besides the constants true and false and pseudo Boolean constraints the
 * atomic formulas in LogicNG.  Each variable is a positive literal.
 * <p>
 * A literal consists of its name and its phase (also sign or polarity in the literature).
 * A new positive literal can be constructed with {@code f.literal("a", true)} or
 * - as a shortcut - {@code f.variable("a")}.  A new negative literal can be constructed
 * with {@code f.literal("a", false)} or if preferred with {@code f.not(f.variable("a"))}
 * or {@code f.variable("a").negate()}.
 * @version 3.0.0
 * @since 1.0
 */
public interface Literal extends Formula, Comparable<Literal> {

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
        return false;
    }

    @Override
    default boolean isAtomicFormula() {
        return true;
    }

    @Override
    default boolean containsVariable(final Variable variable) {
        return variable.name().equals(name());
    }

    @Override
    default boolean evaluate(final Assignment assignment) {
        return assignment.evaluateLit(this);
    }

    @Override
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        return assignment.restrictLit(f, this);
    }

    @Override
    default boolean containsNode(final Formula formula) {
        return equals(formula);
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        final Formula subst = substitution.getSubstitution(variable());
        return subst == null ? this : (phase() ? subst : subst.negate(f));
    }

    /**
     * Returns the name of the literal.
     * @return the name of the literal
     */
    String name();

    /**
     * Returns the phase of the literal.
     * @return the phase of the literal.
     */
    boolean phase();

    /**
     * Returns a positive version of this literal (aka a variable).
     * @return a positive version of this literal
     */
    Variable variable();

    @Override
    Literal negate(final FormulaFactory f);

    @Override
    default int compareTo(final Literal lit) {
        if (this == lit) {
            return 0;
        }
        final int res = name().compareTo(lit.name());
        if (res == 0 && phase() != lit.phase()) {
            return phase() ? -1 : 1;
        }
        return res;
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
