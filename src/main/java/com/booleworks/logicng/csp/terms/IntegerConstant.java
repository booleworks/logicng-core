package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.LinearExpression;

public final class IntegerConstant extends Term implements Comparable<IntegerConstant> {
    private final int value;

    public IntegerConstant(final int value) {
        super(value == 0 ? Term.Type.ZERO : value == 1 ? Term.Type.ONE : Term.Type.CONST);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean isAtom() {
        return true;
    }

    @Override
    protected Decomposition calculateDecomposition() {
        return new Decomposition(new LinearExpression(value));
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof IntegerConstant) {
            return value == ((IntegerConstant) other).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int compareTo(final IntegerConstant o) {
        return Integer.compare(value, o.value);
    }
}
