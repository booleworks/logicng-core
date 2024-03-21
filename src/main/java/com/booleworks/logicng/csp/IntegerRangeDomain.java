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
        return this.lb <= this.ub ? this.ub - this.lb + 1 : 0;
    }

    @Override
    public boolean contains(final int element) {
        return this.lb <= element && element <= this.ub;
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
        return new IntegerRangeDomain(Math.min(this.lb, d.lb), Math.max(this.ub, d.ub));
    }

    @Override
    public IntegerDomain cap(final IntegerDomain d) {
        return d instanceof IntegerRangeDomain ? bound(d.lb, d.ub) : d.bound(this.lb, this.ub);
    }

    @Override
    public IntegerDomain neg() {
        return new IntegerRangeDomain(-this.ub, -this.lb);
    }

    @Override
    public IntegerDomain abs() {
        final int lb0 = Math.min(Math.abs(this.lb), Math.abs(this.ub));
        final int ub0 = Math.max(Math.abs(this.lb), Math.abs(this.ub));
        return this.lb <= 0 && 0 <= this.ub ? new IntegerRangeDomain(0, ub0) : new IntegerRangeDomain(lb0, ub0);
    }

    @Override
    public IntegerDomain add(final int a) {
        return new IntegerRangeDomain(this.lb + a, this.ub + a);
    }

    @Override
    public IntegerDomain add(final IntegerDomain d) {
        if (d.size() == 1) {
            return add(d.lb);
        } else if (size() == 1) {
            return d.add(this.lb);
        }
        return new IntegerRangeDomain(this.lb + d.lb, this.ub + d.ub);
    }

    @Override
    public IntegerDomain mul(final int a) {
        if (size() <= MAX_SET_SIZE) {
            final SortedSet<Integer> d = new TreeSet<>();
            for (int value = this.lb; value <= this.ub; value++) {
                d.add(value * a);
            }
            return create(d);
        } else {
            return a < 0 ? new IntegerRangeDomain(this.ub * a, this.lb * a) : new IntegerRangeDomain(this.lb * a, this.ub * a);
        }
    }

    @Override
    public IntegerDomain mul(final IntegerDomain d) {
        if (d.size() == 1) {
            return mul(d.lb);
        } else if (size() == 1) {
            return d.mul(this.lb);
        }
        return mulRanges(this, d);
    }

    @Override
    public IntegerDomain div(final int a) {
        return a < 0 ? new IntegerRangeDomain(div(this.ub, a), div(this.lb, a)) : new IntegerRangeDomain(div(this.lb, a), div(this.ub, a));
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
        if (this.ub <= d.lb) {
            return this;
        } else if (d.ub <= this.lb) {
            return d;
        }
        return d instanceof IntegerRangeDomain ? new IntegerRangeDomain(Math.min(this.lb, d.lb), Math.min(this.ub, d.ub)) : d.min(this);
    }

    @Override
    public IntegerDomain max(final IntegerDomain d) {
        if (this.lb >= d.ub) {
            return this;
        } else if (d.lb >= this.ub) {
            return d;
        }
        return d instanceof IntegerRangeDomain ? new IntegerRangeDomain(Math.max(this.lb, d.lb), Math.max(this.ub, d.ub)) : d.max(this);
    }

}
