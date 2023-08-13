// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;

import java.util.ArrayList;
import java.util.Collection;
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
public abstract class NAryOperator extends LngCachedFormula {

    protected final Formula[] operands;
    private volatile int hashCode;

    /**
     * Constructor.
     * @param type     the operator's type
     * @param operands the list of operands
     * @param f        the factory which created this instance
     */
    NAryOperator(final FType type, final Collection<? extends Formula> operands, final FormulaFactory f) {
        super(type, f);
        this.operands = operands.toArray(new Formula[0]);
        this.hashCode = 0;
    }

    @Override
    public int numberOfOperands() {
        return operands.length;
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
        for (final Formula op : operands) {
            if (op.containsVariable(variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Formula restrict(final Assignment assignment) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        for (final Formula op : operands) {
            nops.add(op.restrict(assignment));
        }
        return f.naryOperator(type(), nops);
    }

    @Override
    public boolean containsNode(final Formula formula) {
        if (equals(formula)) {
            return true;
        }
        if (type() != formula.type()) {
            for (final Formula op : operands) {
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
        for (final Formula op : operands) {
            fOps.remove(op);
            if (op.containsNode(formula)) {
                return true;
            }
        }
        return fOps.isEmpty();
    }

    @Override
    public Formula substitute(final Substitution substitution) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        for (final Formula op : operands) {
            nops.add(op.substitute(substitution));
        }
        return f.naryOperator(type(), nops);
    }

    @Override
    public Formula negate() {
        return f.not(this);
    }

    /**
     * Helper method for generating the hashcode.
     * @param shift shift value
     * @return hashcode
     */
    protected int hashCode(final int shift) {
        if (hashCode == 0) {
            int temp = 1;
            for (final Formula formula : operands) {
                temp += formula.hashCode();
            }
            temp *= shift;
            hashCode = temp;
        }
        return hashCode;
    }

    protected boolean compareOperands(final Formula[] other) {
        if (operands.length != other.length) {
            return false;
        }
        for (final Formula op1 : operands) {
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
    public Iterator<Formula> iterator() {
        return new Iterator<>() {
            private int i;

            @Override
            public boolean hasNext() {
                return i < NAryOperator.this.operands.length;
            }

            @Override
            public Formula next() {
                if (i == NAryOperator.this.operands.length) {
                    throw new NoSuchElementException();
                }
                return NAryOperator.this.operands[i++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Stream<Formula> stream() {
        return Stream.of(operands);
    }
}
