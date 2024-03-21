package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class NAryFunction extends Function {
    protected final LinkedHashSet<Term> operands;

    NAryFunction(final CspFactory cspFactory, final Term.Type type, final LinkedHashSet<Term> operands) {
        super(cspFactory, type);
        this.operands = operands;
    }

    public Set<Term> getOperands() {
        return Collections.unmodifiableSet(this.operands);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (this.cspFactory == ((NAryFunction) other).cspFactory) {
                return false; // the same factory would have produced a == object
            }
            return Objects.equals(this.operands, ((NAryFunction) other).operands);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.operands);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.type);
        builder.append('<');
        builder.append(this.operands.stream().map(Object::toString).collect(Collectors.joining(", ")));
        builder.append('>');
        return builder.toString();
    }
}
