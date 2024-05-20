package com.booleworks.logicng.csp.terms;

import java.util.Objects;

public abstract class Function extends Term {
    public Function(final Term.Type type) {
        super(type);
    }

    @Override
    public boolean isAtom() {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Function that = (Function) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
