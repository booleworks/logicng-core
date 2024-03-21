package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;

import java.util.Objects;

public abstract class BinaryFunction extends Function {
    protected final Term left;
    protected final Term right;

    BinaryFunction(final CspFactory cspFactory, final Term.Type type, final Term left, final Term right) {
        super(cspFactory, type);
        this.left = left;
        this.right = right;
    }

    public Term getLeft() {
        return this.left;
    }

    public Term getRight() {
        return this.right;
    }

    boolean equals(final Object other, final boolean withOrder) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (this.cspFactory == ((BinaryFunction) other).cspFactory) {
                return false; // the same factory would have produced a == object
            }
            final BinaryFunction that = (BinaryFunction) other;
            return withOrder
                    ? Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right) ||
                    Objects.equals(this.left, that.right) && Objects.equals(this.right, that.left)
                    : Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
        }
        return false;
    }

    int hashCode(final boolean withOrder) {
        return this.type.ordinal() + (withOrder
                ? 11 * this.left.hashCode() - 13 * this.right.hashCode()
                : 17 * this.left.hashCode() + 19 * this.right.hashCode());
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.type);
        builder.append('<');
        builder.append(this.left);
        builder.append(", ");
        builder.append(this.right);
        builder.append('>');
        return builder.toString();
    }
}
