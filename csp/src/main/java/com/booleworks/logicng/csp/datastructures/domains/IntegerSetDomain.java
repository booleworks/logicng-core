// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.datastructures.domains;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An integer set domain consists of a set of given integers and contains
 * only these values.
 * @version 3.0.0
 * @since 3.0.0
 */
public class IntegerSetDomain extends IntegerDomain {
    protected final SortedSet<Integer> values;

    /**
     * Constructs a new integer set domain with the given values.
     * @param values the values
     */
    protected IntegerSetDomain(final SortedSet<Integer> values) {
        super(values.first(), values.last());
        this.values = values;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean contains(final int element) {
        return values.contains(element);
    }

    @Override
    public boolean isContiguous() {
        return false;
    }

    @Override
    public IntegerDomain bound(final int lb, final int ub) {
        if (lb <= this.lb && this.ub <= ub) {
            return this;
        }
        return IntegerDomain.of(values.subSet(lb, ub + 1));
    }

    @Override
    public Iterator<Integer> values(final int lb, final int ub) {
        if (lb > ub) {
            return Collections.emptyIterator();
        } else {
            return values.subSet(lb, ub + 1).iterator();
        }
    }

    @Override
    public IntegerDomain cup(final IntegerDomain d) {
        if (d instanceof IntegerSetDomain) {
            final SortedSet<Integer> newValues = new TreeSet<>(values);
            newValues.addAll(((IntegerSetDomain) d).values);
            return create(newValues);
        } else {
            return IntegerDomain.of(Math.min(lb, d.lb), Math.max(ub, d.ub));
        }
    }

    @Override
    public IntegerDomain cap(final IntegerDomain d) {
        if (d instanceof IntegerRangeDomain) {
            return bound(d.lb, d.ub);
        } else {
            final SortedSet<Integer> newValues = new TreeSet<>();
            for (final int value : values) {
                if (d.contains(value)) {
                    newValues.add(value);
                }
            }
            return IntegerDomain.of(newValues);
        }
    }

    @Override
    public IntegerDomain neg() {
        final SortedSet<Integer> d = new TreeSet<>();
        for (final int value : values) {
            d.add(-value);
        }
        return create(d);
    }

    @Override
    public IntegerDomain abs() {
        final SortedSet<Integer> d = new TreeSet<>();
        for (final int value : values) {
            d.add(Math.abs(value));
        }
        return create(d);
    }

    @Override
    public IntegerDomain add(final int a) {
        final SortedSet<Integer> d = new TreeSet<>();
        for (final int value : values) {
            d.add(value + a);
        }
        return create(d);
    }

    @Override
    public IntegerDomain add(final IntegerDomain d) {
        if (d.size() == 1) {
            return add(d.lb);
        } else if (size() == 1) {
            return d.add(lb);
        }
        if (d instanceof IntegerRangeDomain) {
            return IntegerDomain.of(lb + d.lb, ub + d.ub);
        } else {
            final SortedSet<Integer> newValues = new TreeSet<>();
            for (final int value1 : values) {
                for (final int value2 : ((IntegerSetDomain) d).values) {
                    newValues.add(value1 + value2);
                }
            }
            return create(newValues);
        }
    }

    @Override
    public IntegerDomain mul(final int a) {
        final SortedSet<Integer> d = new TreeSet<>();
        for (final int value : values) {
            d.add(value * a);
        }
        return create(d);
    }

    @Override
    public IntegerDomain mul(final IntegerDomain d) {
        if (d.size() == 1) {
            return mul(d.lb);
        } else if (size() == 1) {
            return d.mul(lb);
        }
        if (d instanceof IntegerRangeDomain || size() * d.size() > MAX_SET_SIZE) {
            return mulRanges(this, d);
        } else {
            final SortedSet<Integer> newValues = new TreeSet<>();
            for (final int value1 : values) {
                for (final int value2 : ((IntegerSetDomain) d).values) {
                    newValues.add(value1 * value2);
                }
            }
            return create(newValues);
        }
    }

    @Override
    public IntegerDomain div(final int a) {
        final SortedSet<Integer> d = new TreeSet<>();
        for (final int value : values) {
            d.add(div(value, a));
        }
        return create(d);
    }

    @Override
    public IntegerDomain div(final IntegerDomain d) {
        if (d.size() == 1) {
            return div(d.lb);
        }
        if (d instanceof IntegerRangeDomain || size() * d.size() > MAX_SET_SIZE) {
            return divRanges(this, d);
        } else {
            final SortedSet<Integer> newValues = new TreeSet<>();
            for (final int value1 : values) {
                for (final int value2 : ((IntegerSetDomain) d).values) {
                    newValues.add(div(value1, value2));
                }
            }
            return create(newValues);
        }
    }

    @Override
    public IntegerDomain mod(int a) {
        a = Math.abs(a);
        final SortedSet<Integer> d = new TreeSet<>();
        for (final int value : values) {
            d.add(value % a);
        }
        return create(d);
    }

    @Override
    public IntegerDomain mod(final IntegerDomain d) {
        if (d.size() == 1) {
            return mod(d.lb);
        }
        if (d instanceof IntegerRangeDomain) {
            return IntegerDomain.of(0, Math.max(Math.abs(d.lb), Math.abs(d.ub)) - 1);
        } else {
            final SortedSet<Integer> d0 = new TreeSet<>();
            for (final int value1 : values) {
                for (final int value2 : ((IntegerSetDomain) d).values) {
                    d0.add(value1 % value2);
                }
            }
            return create(d0);
        }
    }

    @Override
    public IntegerDomain min(final IntegerDomain d) {
        if (ub <= d.lb) {
            return this;
        } else if (d.ub <= lb) {
            return d;
        }
        final int lb0 = Math.min(lb, d.lb);
        final int ub0 = Math.min(ub, d.ub);
        return generateMinMaxRange(d, lb0, ub0);
    }

    @Override
    public IntegerDomain max(final IntegerDomain d) {
        if (lb >= d.ub) {
            return this;
        } else if (d.lb >= ub) {
            return d;
        }
        final int lb0 = Math.max(lb, d.lb);
        final int ub0 = Math.max(ub, d.ub);
        return generateMinMaxRange(d, lb0, ub0);
    }

    @Override
    public SortedSet<Integer> headSet(final int value) {
        return values.headSet(value);
    }

    private IntegerDomain generateMinMaxRange(final IntegerDomain d, final int lb0, final int ub0) {
        if (d instanceof IntegerRangeDomain) {
            return create(values.subSet(lb0, ub0 + 1));
        } else {
            SortedSet<Integer> newValues = new TreeSet<>(values);
            newValues.addAll(((IntegerSetDomain) d).values);
            newValues = newValues.subSet(lb0, ub0 + 1);
            return create(newValues);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IntegerSetDomain that = (IntegerSetDomain) o;

        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return values != null ? values.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append('{');
        for (final int v : values) {
            b.append(v);
            b.append(',');
        }
        b.setCharAt(b.length() - 1, '}');
        return b.toString();
    }
}

