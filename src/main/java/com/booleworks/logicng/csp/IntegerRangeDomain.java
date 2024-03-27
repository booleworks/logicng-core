package com.booleworks.logicng.csp;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An integer range domain consists only of a lower and an upper bound and
 * contains all values between these values, including the two bounds.
 * @version 3.0.0
 * @since 3.0.0
 */
public class IntegerRangeDomain extends IntegerDomain {

    /**
     * Constructs a new integer range domain with a lower and an upper bound
     * @param lb the lower bound
     * @param ub the upper bound
     */
    public IntegerRangeDomain(final int lb, final int ub) {
        super(lb, ub);
    }

    @Override
    public int size() {
        return lb <= ub ? ub - lb + 1 : 0;
    }

    @Override
    public boolean contains(final int element) {
        return lb <= element && element <= ub;
    }

    @Override
    public boolean isContiguous() {
        return true;
    }

    @Override
    public IntegerRangeDomain bound(final int lb, final int ub) {
        return lb <= this.lb && this.ub <= ub ? this : new IntegerRangeDomain(Math.max(this.lb, lb), Math.min(this.ub, ub));
    }

    @Override
    public Iterator<Integer> values(final int lb, final int ub) {
        return lb > ub ? new Iter(lb, ub) : new Iter(Math.max(lb, this.lb), Math.min(ub, this.ub));
    }

    @Override
    public IntegerDomain cup(final IntegerDomain d) {
        return new IntegerRangeDomain(Math.min(lb, d.lb), Math.max(ub, d.ub));
    }

    @Override
    public IntegerDomain cap(final IntegerDomain d) {
        return d instanceof IntegerRangeDomain ? bound(d.lb, d.ub) : d.bound(lb, ub);
    }

    @Override
    public IntegerDomain neg() {
        return new IntegerRangeDomain(-ub, -lb);
    }

    @Override
    public IntegerDomain abs() {
        final int lb0 = Math.min(Math.abs(lb), Math.abs(ub));
        final int ub0 = Math.max(Math.abs(lb), Math.abs(ub));
        return lb <= 0 && 0 <= ub ? new IntegerRangeDomain(0, ub0) : new IntegerRangeDomain(lb0, ub0);
    }

    @Override
    public IntegerDomain add(final int a) {
        return new IntegerRangeDomain(lb + a, ub + a);
    }

    @Override
    public IntegerDomain add(final IntegerDomain d) {
        if (d.size() == 1) {
            return add(d.lb);
        } else if (size() == 1) {
            return d.add(lb);
        }
        return new IntegerRangeDomain(lb + d.lb, ub + d.ub);
    }

    @Override
    public IntegerDomain mul(final int a) {
        if (size() <= MAX_SET_SIZE) {
            final SortedSet<Integer> d = new TreeSet<>();
            for (int value = lb; value <= ub; value++) {
                d.add(value * a);
            }
            return create(d);
        } else {
            return a < 0 ? new IntegerRangeDomain(ub * a, lb * a) : new IntegerRangeDomain(lb * a, ub * a);
        }
    }

    @Override
    public IntegerDomain mul(final IntegerDomain d) {
        if (d.size() == 1) {
            return mul(d.lb);
        } else if (size() == 1) {
            return d.mul(lb);
        }
        return mulRanges(this, d);
    }

    @Override
    public IntegerDomain div(final int a) {
        return a < 0 ? new IntegerRangeDomain(div(ub, a), div(lb, a)) : new IntegerRangeDomain(div(lb, a), div(ub, a));
    }

    @Override
    public IntegerDomain div(final IntegerDomain d) {
        if (d.size() == 1) {
            return div(d.lb);
        }
        return divRanges(this, d);
    }

    @Override
    public IntegerDomain mod(int a) {
        a = Math.abs(a);
        return new IntegerRangeDomain(0, a - 1);
    }

    @Override
    public IntegerDomain mod(final IntegerDomain d) {
        return d.size() == 1 ? mod(d.lb) : new IntegerRangeDomain(0, Math.max(Math.abs(d.lb), Math.abs(d.ub)) - 1);
    }

    @Override
    public IntegerDomain min(final IntegerDomain d) {
        if (ub <= d.lb) {
            return this;
        } else if (d.ub <= lb) {
            return d;
        }
        return d instanceof IntegerRangeDomain ? new IntegerRangeDomain(Math.min(lb, d.lb), Math.min(ub, d.ub)) : d.min(this);
    }

    @Override
    public IntegerDomain max(final IntegerDomain d) {
        if (lb >= d.ub) {
            return this;
        } else if (d.lb >= ub) {
            return d;
        }
        return d instanceof IntegerRangeDomain ? new IntegerRangeDomain(Math.max(lb, d.lb), Math.max(ub, d.ub)) : d.max(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        final IntegerRangeDomain that = (IntegerRangeDomain) o;

        return ub == that.ub && lb == that.lb;
    }
}
