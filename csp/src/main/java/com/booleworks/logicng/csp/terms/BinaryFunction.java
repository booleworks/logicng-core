package com.booleworks.logicng.csp.terms;

import java.util.Objects;
import java.util.SortedSet;

/**
 * An arithmetic function term with exactly two operands.
 */
public abstract class BinaryFunction extends Function {
    /**
     * Left operand.
     */
    protected final Term left;

    /**
     * Right operand.
     */
    protected final Term right;

    /**
     * Constructs a new binary function of the given type and operands.
     * @param type  the type of the term
     * @param left  the left operand
     * @param right the right operand
     */
    BinaryFunction(final Term.Type type, final Term left, final Term right) {
        super(type);
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the left operand.
     * @return the left operand
     */
    public Term getLeft() {
        return left;
    }

    /**
     * Returns the right operand.
     * @return the right operand
     */
    public Term getRight() {
        return right;
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        left.variablesInplace(variables);
        right.variablesInplace(variables);
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
