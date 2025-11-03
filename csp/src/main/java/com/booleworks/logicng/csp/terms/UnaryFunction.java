package com.booleworks.logicng.csp.terms;

import java.util.Objects;
import java.util.SortedSet;

/**
 * An arithmetic function term with exactly one operand.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class UnaryFunction extends Function {
    /**
     * The operand of this term.
     */
    protected final Term operand;

    /**
     * Constructs a new unary function of a given type and operand.
     * @param type    the type of the term
     * @param operand the operand
     */
    UnaryFunction(final Term.Type type, final Term operand) {
        super(type);
        this.operand = operand;
    }

    /**
     * Get the operand of this function.
     * @return the operand of this function
     */
    public Term getOperand() {
        return operand;
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        operand.variablesInplace(variables);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
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
        return Type.toPrefixIdentifier(type)
                + '('
                + operand
                + ')';
    }
}
