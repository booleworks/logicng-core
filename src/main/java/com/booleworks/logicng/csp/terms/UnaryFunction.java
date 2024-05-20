package com.booleworks.logicng.csp.terms;

import java.util.Objects;

public abstract class UnaryFunction extends Function {
    protected final Term operand;

    UnaryFunction(final Term.Type type, final Term operand) {
        super(type);
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
