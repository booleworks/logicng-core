package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.terms.Term;

import java.util.Objects;

public abstract class BinaryPredicate extends CspPredicate {

    protected Term left;
    protected Term right;

    BinaryPredicate(final CspFactory f, final CspPredicate.Type type, final Term left, final Term right) {
        super(f, type);
        this.left = left;
        this.right = right;
    }

    public Term getLeft() {
        return this.left;
    }

    public Term getRight() {
        return this.right;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (this.factory() == ((BinaryPredicate) other).factory()) {
                return false; // the same factory would have produced a == object
            }
            final BinaryPredicate that = (BinaryPredicate) other;
            return this.type == that.type && Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.left, this.right);
    }

    @Override
    public String toString() {
        return this.type + "(" + this.left + ", " + this.right + ")";
    }
}
