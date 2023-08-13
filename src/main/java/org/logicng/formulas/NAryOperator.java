// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Super class for Boolean n-ary operators.
 * @version 3.0.0
 * @since 1.0
 */
public interface NAryOperator extends Formula {

    List<Formula> operands();

    @Override
    default int numberOfOperands() {
        return operands().size();
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
        for (final Formula op : operands()) {
            if (op.containsVariable(variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default Formula restrict(final Assignment assignment) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        for (final Formula op : operands()) {
            nops.add(op.restrict(assignment));
        }
        return factory().naryOperator(type(), nops);
    }

    @Override
    default boolean containsNode(final Formula formula) {
        if (equals(formula)) {
            return true;
        }
        if (type() != formula.type()) {
            for (final Formula op : operands()) {
                if (op.containsNode(formula)) {
                    return true;
                }
            }
            return false;
        }
        final List<Formula> fOps = new ArrayList<>(formula.numberOfOperands());
        for (final Formula op : formula) {
            fOps.add(op);
        }
        for (final Formula op : operands()) {
            fOps.remove(op);
            if (op.containsNode(formula)) {
                return true;
            }
        }
        return fOps.isEmpty();
    }

    @Override
    default Formula substitute(final Substitution substitution) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        for (final Formula op : operands()) {
            nops.add(op.substitute(substitution));
        }
        return factory().naryOperator(type(), nops);
    }

    @Override
    default Formula negate() {
        return factory().not(this);
    }

    default boolean compareOperands(final List<Formula> other) {
        if (operands().size() != other.size()) {
            return false;
        }
        for (final Formula op1 : operands()) {
            boolean found = false;
            for (final Formula op2 : other) {
                if (op1.equals(op2)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    default Iterator<Formula> iterator() {
        return new Iterator<>() {
            private int i;

            @Override
            public boolean hasNext() {
                return i < NAryOperator.this.operands().size();
            }

            @Override
            public Formula next() {
                if (i == NAryOperator.this.operands().size()) {
                    throw new NoSuchElementException();
                }
                return NAryOperator.this.operands().get(i++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    default Stream<Formula> stream() {
        return operands().stream();
    }
}
