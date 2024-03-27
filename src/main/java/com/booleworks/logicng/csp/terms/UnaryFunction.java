package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;

import java.util.Objects;

public abstract class UnaryFunction extends Function {
    protected final Term operand;

    UnaryFunction(final CspFactory cspFactory, final Term.Type type, final Term operand) {
        super(cspFactory, type);
        this.operand = operand;
    }

    public Term getOperand() {
        return operand;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (cspFactory == ((UnaryFunction) other).cspFactory) {
                return false; // the same factory would have produced a == object
            }
            return Objects.equals(operand, ((UnaryFunction) other).operand);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, operand);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append('<');
        builder.append(operand);
        builder.append('>');
        return builder.toString();
    }
}
