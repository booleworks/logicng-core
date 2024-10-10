package com.booleworks.logicng.csp.terms;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * An arithmetic function term with any number of operands.
 */
public abstract class NAryFunction extends Function {
    /**
     * The operands of this function.
     */
    protected final LinkedHashSet<Term> operands;

    /**
     * Constructs a new N-ary function of the given type and operands.
     * @param type     the type of this term
     * @param operands the operands
     */
    NAryFunction(final Term.Type type, final LinkedHashSet<Term> operands) {
        super(type);
        this.operands = operands;
    }

    /**
     * Returns the operands of the term.
     * @return the operands of the term
     */
    public Set<Term> getOperands() {
        return Collections.unmodifiableSet(operands);
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        for (final Term op : operands) {
            op.variablesInplace(variables);
        }
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
