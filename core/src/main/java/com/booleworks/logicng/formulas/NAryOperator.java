// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;

import java.util.ArrayList;
import java.util.HashSet;
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

    int BOUNDARY_SET_CREATION_EQUALS = 20;

    List<Formula> getOperands();

    @Override
    default int numberOfOperands() {
        return getOperands().size();
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
        for (final Formula op : getOperands()) {
            if (op.containsVariable(variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        for (final Formula op : getOperands()) {
            nops.add(op.restrict(f, assignment));
        }
        return f.naryOperator(getType(), nops);
    }

    @Override
    default boolean containsNode(final Formula formula) {
        if (equals(formula)) {
            return true;
        }
        if (getType() != formula.getType()) {
            for (final Formula op : getOperands()) {
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
        for (final Formula op : getOperands()) {
            fOps.remove(op);
            if (op.containsNode(formula)) {
                return true;
            }
        }
        return fOps.isEmpty();
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        for (final Formula op : getOperands()) {
            nops.add(op.substitute(f, substitution));
        }
        return f.naryOperator(getType(), nops);
    }

    @Override
    default Formula negate(final FormulaFactory f) {
        return f.not(this);
    }

    default boolean compareOperands(final List<Formula> other) {
        if (getOperands().size() != other.size()) {
            return false;
        }
        if (getOperands().size() > BOUNDARY_SET_CREATION_EQUALS) {
            final HashSet<Formula> otherSet = new HashSet<>(other);
            for (final Formula op : getOperands()) {
                if (!otherSet.contains(op)) {
                    return false;
                }
            }
        } else {
            for (final Formula op1 : getOperands()) {
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
        }
        return true;
    }

    @Override
    default Iterator<Formula> iterator() {
        return new Iterator<>() {
            private int i;

            @Override
            public boolean hasNext() {
                return i < getOperands().size();
            }

            @Override
            public Formula next() {
                if (i == getOperands().size()) {
                    throw new NoSuchElementException();
                }
                return getOperands().get(i++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    default Stream<Formula> stream() {
        return getOperands().stream();
    }

    default int computeHash(final int shift) {
        int hashcode = 1;
        for (final Formula formula : getOperands()) {
            hashcode += formula.hashCode();
        }
        hashcode *= shift;
        return hashcode;
    }
}
