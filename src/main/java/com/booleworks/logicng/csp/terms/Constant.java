package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.LinearExpression;

public final class Constant extends Term implements Comparable<Constant> {
    private final int value;

    public Constant(final CspFactory cspFactory, final int value) {
        super(cspFactory, value == 0 ? Term.Type.ZERO : value == 1 ? Term.Type.ONE : Term.Type.CONST);
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
        if (other instanceof Term && cspFactory == ((Term) other).cspFactory) {
            return false; // the same factory would have produced a == object
        }
        if (other instanceof Constant) {
            return value == ((Constant) other).value;
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
    public int compareTo(final Constant o) {
        return Integer.compare(value, o.value);
    }
}
