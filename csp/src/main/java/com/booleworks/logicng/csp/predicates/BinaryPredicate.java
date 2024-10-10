package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.Objects;
import java.util.SortedSet;

/**
 * Represents the class of predicates that have exactly two terms as operands.
 */
public abstract class BinaryPredicate extends CspPredicate {

    /**
     * left operand
     */
    protected Term left;

    /**
     * right operand
     */
    protected Term right;

    BinaryPredicate(final CspPredicate.Type type, final Term left, final Term right, final FormulaFactory f) {
        super(type, f);
        this.left = left;
        this.right = right;
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        left.variablesInplace(variables);
        right.variablesInplace(variables);
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
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            final BinaryPredicate that = (BinaryPredicate) other;
            return type == that.type && Objects.equals(left, that.left) && Objects.equals(right, that.right);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, left, right);
    }

    @Override
    public String toString() {
        return type + "(" + left + ", " + right + ")";
    }
}
