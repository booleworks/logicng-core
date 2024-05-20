package com.booleworks.logicng.csp.terms;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class NAryFunction extends Function {
    protected final LinkedHashSet<Term> operands;

    NAryFunction(final Term.Type type, final LinkedHashSet<Term> operands) {
        super(type);
        this.operands = operands;
    }

    public Set<Term> getOperands() {
        return Collections.unmodifiableSet(operands);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            return Objects.equals(operands, ((NAryFunction) other).operands);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, operands);
    }

    @Override
    public String toString() {
        return String.valueOf(type) +
                '<' +
                operands.stream().map(Object::toString).collect(Collectors.joining(", ")) +
                '>';
    }
}
