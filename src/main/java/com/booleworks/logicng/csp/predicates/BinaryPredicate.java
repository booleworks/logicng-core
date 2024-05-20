package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.terms.Term;

import java.util.Objects;

public abstract class BinaryPredicate extends CspPredicate {

    protected Term left;
    protected Term right;

    BinaryPredicate(final CspPredicate.Type type, final Term left, final Term right) {
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
