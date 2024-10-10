package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;

import java.util.Collections;
import java.util.SortedSet;

/**
 * An integer constant.
 */
public final class IntegerConstant extends Term implements IntegerHolder {
    private final int value;

    /**
     * Constructs a new integer constant.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new terms.
     * @param value the constant value
     */
    public IntegerConstant(final int value) {
        super(value == 0 ? Term.Type.ZERO : value == 1 ? Term.Type.ONE : Term.Type.CONST);
        this.value = value;
    }

    /**
     * Returns the value of the constant.
     * @return the value of the constant
     */
    public int getValue() {
        return value;
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
    }

    @Override
    public boolean isAtom() {
        return true;
    }

    @Override
    protected Decomposition calculateDecomposition(final CspFactory cf) {
        return new Decomposition(new LinearExpression(value), Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet());
    }

    @Override
    public IntegerDomain getDomain() {
        return IntegerDomain.of(value, value);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof IntegerConstant) {
            return value == ((IntegerConstant) other).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
