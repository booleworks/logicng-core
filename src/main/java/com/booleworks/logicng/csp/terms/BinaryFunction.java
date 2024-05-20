package com.booleworks.logicng.csp.terms;

import java.util.Objects;

public abstract class BinaryFunction extends Function {
    protected final Term left;
    protected final Term right;

    BinaryFunction(final Term.Type type, final Term left, final Term right) {
        super(type);
        this.left = left;
        this.right = right;
    }

    public Term getLeft() {
        return left;
    }

    public Term getRight() {
        return right;
    }

    boolean equals(final Object other, final boolean withOrder) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            final BinaryFunction that = (BinaryFunction) other;
            return withOrder
                    ? Objects.equals(left, that.left) && Objects.equals(right, that.right) ||
                    Objects.equals(left, that.right) && Objects.equals(right, that.left)
                    : Objects.equals(left, that.left) && Objects.equals(right, that.right);
        }
        return false;
    }

    int hashCode(final boolean withOrder) {
        return type.ordinal() + (withOrder
                ? 11 * left.hashCode() - 13 * right.hashCode()
                : 17 * left.hashCode() + 19 * right.hashCode());
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(type);
        builder.append('<');
        builder.append(left);
        builder.append(", ");
        builder.append(right);
        builder.append('>');
        return builder.toString();
    }
}
