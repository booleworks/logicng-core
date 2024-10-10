package com.booleworks.logicng.csp.datastructures.domains;

import java.util.Iterator;
import java.util.SortedSet;

/**
 * Super class for integer domains for constraints.  An integer domain can be contiguous
 * and thus only defined by a lower and upper bound ({@link IntegerRangeDomain}), or
 * defined by a set of concrete values ({@link IntegerSetDomain}).
 */
public abstract class IntegerDomain {
    /**
     * Maximum number of individual elements in a domain.
     */
    public static int MAX_SET_SIZE = 128;

    /**
     * The lower bound of the domain
     */
    protected final int lb;

    /**
     * The upper bound of the domain
     */
    protected final int ub;

    /**
     * Creates a domain of a set of integers.
     * <p>
     * If the set is continuous it creates an {@link IntegerRangeDomain} otherwise an {@link IntegerSetDomain}.
     * @param values the values
     * @return new domain
     */
    public static IntegerDomain of(final SortedSet<Integer> values) {
        assert values.comparator() == null : "Custom comparators are not supported";
        if (values.isEmpty()) {
            return IntegerDomain.of(0, -1);
        }
        final int lb = values.first();
        final int ub = values.last();
        if (values.size() == ub - lb + 1) {
            return IntegerDomain.of(lb, ub);
        } else {
            return new IntegerSetDomain(values);
        }
    }

    /**
     * Creates a range domain from the lower bound and upper bound.
     * @param lb the lower bound
     * @param ub the upper bound
     * @return new domain
     */
    public static IntegerDomain of(final int lb, final int ub) {
        return new IntegerRangeDomain(lb, ub);
    }

    /**
     * Constructs a new domain from lower bound and upper bound.
     * @param lb the lower bound
     * @param ub the upper bound
     */
    protected IntegerDomain(final int lb, final int ub) {
        this.lb = lb;
        this.ub = ub;
    }

    /**
     * Returns the domain size.
     * @return the domain size
     */
    public abstract int size();

    /**
     * Returns whether the domain is empty or not.
     * @return whether the domain is empty or not
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns whether the domain contains the given element.
     * @param element the element to search for
     * @return true if the element is contained in the domain, false otherwise
     */
    public abstract boolean contains(final int element);

    /**
     * Returns whether the domain is contiguous thus a range domain or not and
     * thus a set domain.
     * @return whether the domain is contiguous
     */
    public abstract boolean isContiguous();

    /**
     * Returns a new domain containing all elements of this domain but bound by the new
     * given lower and upper bound.
     * @param lb the new lower bound
     * @param ub the new upper bound
     * @return the new bound domain
     */
    public abstract IntegerDomain bound(int lb, int ub);

    /**
     * Returns the values of this domain bound to a lower and upper bound as an iterator.
     * @param lb the lower bound
     * @param ub the upper bound
     * @return the iterator for the bound values
     */
    public abstract Iterator<Integer> values(int lb, int ub);

    /**
     * Returns a new domain which is this domain united with the given one.
     * @param d1 the other domain
     * @return the union of this and the other domain
     */
    public abstract IntegerDomain cup(IntegerDomain d1);

    /**
     * Returns a new domain which is this domain intersected with the given one.
     * @param d1 the other domain
     * @return the intersection of this and the other domain
     */
    public abstract IntegerDomain cap(IntegerDomain d1);

    /**
     * Returns a new domain where every element of this domain is negated.
     * @return a new domain where every element is negated
     */
    public abstract IntegerDomain neg();

    /**
     * Returns a new domain with the absolute values of all elements in this domain.
     * @return a new domain with the absolute values
     */
    public abstract IntegerDomain abs();

    /**
     * Returns a new domain where each value of this domain is increased by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain add(final int a);

    /**
     * Returns a new domain with all elements resulting in the addition of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the added values
     */
    public abstract IntegerDomain add(final IntegerDomain d);

    /**
     * Returns a new domain where each value of this domain is decreased by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public IntegerDomain sub(final int a) {
        return add(-a);
    }

    /**
     * Returns a new domain with all elements resulting in the subtraction of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the subtracted values
     */
    public IntegerDomain sub(final IntegerDomain d) {
        return add(d.neg());
    }

    /**
     * Returns a new domain where each value of this domain is multiplied by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain mul(int a);

    /**
     * Returns a new domain with all elements resulting in the multiplication of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the multiplied values
     */
    public abstract IntegerDomain mul(final IntegerDomain d);

    /**
     * Returns a new domain where each value of this domain is divided by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain div(int a);

    /**
     * Returns a new domain with all elements resulting in the division of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the divided values
     */
    public abstract IntegerDomain div(final IntegerDomain d);

    /**
     * Returns a new domain where each value of this domain is taken modulo by a constant value
     * @param a a constant integer
     * @return a new domain with the new values
     */
    public abstract IntegerDomain mod(int a);

    /**
     * Returns a new domain with all elements resulting in the modulation of elements of this
     * domain and another given domain.
     * @param d the other domain
     * @return a new domain with the moduled values
     */
    public abstract IntegerDomain mod(final IntegerDomain d);

    /**
     * Returns a new domain with all elements when taking the minimum of this domain with another domain.
     * @param d the other domain
     * @return a new domain with the min values
     */
    public abstract IntegerDomain min(final IntegerDomain d);

    /**
     * Returns a new domain with all elements when taking the maximum of this domain with another domain.
     * @param d the other domain
     * @return a new domain with the max values
     */
    public abstract IntegerDomain max(final IntegerDomain d);

    /**
     * Returns the subset whose elements are strictly less than {@code value}.
     * @param value the upper bound (excluded)
     * @return the subset
     */
    public abstract SortedSet<Integer> headSet(final int value);

    /**
     * Returns the lower bound of this domain.
     * @return the lower bound
     */
    public int lb() {
        return lb;
    }

    /**
     * Returns the upper bound of this domain.
     * @return the upper bound
     */
    public int ub() {
        return ub;
    }

    /**
     * Returns an iterator with all values contained in the domain.
     * @return iterator with all values contained in the domain.
     */
    public Iterator<Integer> iterator() {
        return values(lb, ub);
    }

    /**
     * Creates a new domain from a set of integers. This function restricts the size of {@link IntegerSetDomain}s and
     * will approximate them with a {@link IntegerRangeDomain} if it becomes to large.
     * @param domain the set of integers
     * @return integer domain
     */
    protected static IntegerDomain create(final SortedSet<Integer> domain) {
        final int lb = domain.first();
        final int ub = domain.last();
        if (domain.size() <= MAX_SET_SIZE) {
            return IntegerDomain.of(domain);
        } else {
            return IntegerDomain.of(lb, ub);
        }
    }

    /**
     * Multiples two ranges (only lower and upper bound, not individual values).
     * @param a first range
     * @param b second range
     * @return multiplied range as ranged domain
     */
    protected static IntegerDomain mulRanges(final IntegerDomain a, final IntegerDomain b) {
        final int b00 = a.lb * b.lb;
        final int b01 = a.lb * b.ub;
        final int b10 = a.ub * b.lb;
        final int b11 = a.ub * b.ub;
        final int lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
        final int ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
        return IntegerDomain.of(lb0, ub0);
    }

    /**
     * Divide two ranges (only lower and upper bound, not individual values).
     * @param a the dividend
     * @param b the divisor
     * @return divided range as ranged domain
     */
    protected static IntegerDomain divRanges(final IntegerDomain a, final IntegerDomain b) {
        final int b00 = div(a.lb, b.lb);
        final int b01 = div(a.lb, b.ub);
        final int b10 = div(a.ub, b.lb);
        final int b11 = div(a.ub, b.ub);
        int lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
        int ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
        if (b.lb <= 1 && 1 <= b.ub) {
            lb0 = Math.min(lb0, Math.min(a.lb, a.ub));
            ub0 = Math.max(ub0, Math.max(a.lb, a.ub));
        }
        if (b.lb <= -1 && -1 <= b.ub) {
            lb0 = Math.min(lb0, Math.min(-a.lb, -a.ub));
            ub0 = Math.max(ub0, Math.max(-a.lb, -a.ub));
        }
        return IntegerDomain.of(lb0, ub0);
    }

    /**
     * divide and round two integer values
     * @param x the dividend
     * @param y the divisor
     * @return the result
     */
    protected static int div(final int x, final int y) {
        return x < 0 && x % y != 0 ? x / y - 1 : x / y;
    }
}
