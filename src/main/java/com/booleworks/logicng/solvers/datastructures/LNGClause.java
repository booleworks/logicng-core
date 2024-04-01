// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * MiniSat -- Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.solvers.datastructures;

import com.booleworks.logicng.collections.LNGIntVector;

import java.util.Comparator;

/**
 * A clause of the SAT solver.
 * @version 2.0.0
 * @since 1.0
 */
public final class LNGClause {

    /**
     * A comparator for clauses based on LBD and activity (used for the Glucose solver).
     */
    public static final Comparator<LNGClause> glucoseComparator = (x, y) -> {
        if (x.size() > 2 && y.size() == 2) {
            return -1;
        }
        if (y.size() > 2 && x.size() == 2) {
            return 1;
        }
        if (x.size() == 2 && y.size() == 2) {
            return 1;
        }
        if (x.lbd() > y.lbd()) {
            return -1;
        }
        if (x.lbd() < y.lbd()) {
            return 1;
        }
        return x.activity() < y.activity() ? -1 : 1;
    };

    private final LNGIntVector data;
    private final int learntOnState;
    private final boolean isAtMost;
    private double activity;
    private boolean seen;
    private long lbd;
    private boolean canBeDel;
    private boolean oneWatched;
    private int atMostWatchers;

    /**
     * Constructs a new clause
     * @param ps            the vector of literals
     * @param learntOnState the index of the solver state on which this clause was learnt
     *                      or -1 if it is not a learnt clause
     */
    public LNGClause(final LNGIntVector ps, final int learntOnState) {
        this(ps, learntOnState, false);
    }

    /**
     * Constructs a new clause
     * @param ps            the vector of literals
     * @param learntOnState the index of the solver state on which this clause was learnt
     *                      or -1 if it is not a learnt clause
     * @param isAtMost      {@code true} if it is an at-most clause, {@code false} otherwise
     */
    public LNGClause(final LNGIntVector ps, final int learntOnState, final boolean isAtMost) {
        data = new LNGIntVector(ps.size());
        for (int i = 0; i < ps.size(); i++) {
            data.unsafePush(ps.get(i));
        }
        this.learntOnState = learntOnState;
        seen = false;
        lbd = 0;
        canBeDel = true;
        oneWatched = false;
        this.isAtMost = isAtMost;
        atMostWatchers = -1;
    }

    LNGClause(final LNGIntVector data, final int learntOnState, final boolean isAtMost, final double activity, final boolean seen,
              final long lbd, final boolean canBeDel, final boolean oneWatched, final int atMostWatchers) {
        this.data = data;
        this.learntOnState = learntOnState;
        this.isAtMost = isAtMost;
        this.activity = activity;
        this.seen = seen;
        this.lbd = lbd;
        this.canBeDel = canBeDel;
        this.oneWatched = oneWatched;
        this.atMostWatchers = atMostWatchers;
    }

    /**
     * Returns the size (number of literals) of this clause.
     * @return the size
     */
    public int size() {
        return data.size();
    }

    /**
     * Returns the literal at index {@code i}.
     * @param i the index
     * @return the literal at index {@code i}
     */
    public int get(final int i) {
        return data.get(i);
    }

    /**
     * Sets the literal at index {@code i}.
     * @param i   the index
     * @param lit the literal
     */
    public void set(final int i, final int lit) {
        data.set(i, lit);
    }

    /**
     * Returns the activity of this clause.
     * @return the activity of this clause
     */
    public double activity() {
        return activity;
    }

    /**
     * Increments this clause's activity by a given value
     * @param inc the increment value
     */
    public void incrementActivity(final double inc) {
        activity += inc;
    }

    /**
     * Rescales this clause's activity
     */
    public void rescaleActivity() {
        activity *= 1e-20;
    }

    /**
     * Returns the solver state on which this clause was learnt, or -1 if it is not a learnt clause.
     * @return the solver state on which this clause was learnt
     */
    public int getLearntOnState() {
        return learntOnState;
    }

    /**
     * Returns {@code true} if this clause is learnt, {@code false} otherwise.
     * @return {@code true} if this clause is learnt
     */
    public boolean learnt() {
        return learntOnState >= 0;
    }

    /**
     * Returns {@code true} if this clause is marked 'seen', {@code false} otherwise.
     * @return {@code true} if this clause is marked 'seen'
     */
    public boolean seen() {
        return seen;
    }

    /**
     * Marks this clause with the given 'seen' flag.
     * @param seen the 'seen' flag
     */
    public void setSeen(final boolean seen) {
        this.seen = seen;
    }

    /**
     * Returns the LBD of this clause.
     * @return the LBD of this clause
     */
    public long lbd() {
        return lbd;
    }

    /**
     * Sets the LBD of this clause.
     * @param lbd the LBD of this clause
     */
    public void setLBD(final long lbd) {
        this.lbd = lbd;
    }

    /**
     * Returns {@code true} if this clause can be deleted, {@code false} otherwise.
     * @return {@code true} if this clause can be deleted
     */
    public boolean canBeDel() {
        return canBeDel;
    }

    /**
     * Sets whether this clause can be deleted or not.
     * @param canBeDel {@code true} if it can be deleted, {@code false} otherwise
     */
    public void setCanBeDel(final boolean canBeDel) {
        this.canBeDel = canBeDel;
    }

    /**
     * Returns {@code true} if this clause is a one literal watched clause, {@code false} otherwise
     * @return {@code true} if this clause is a one literal watched clause
     */
    public boolean oneWatched() {
        return oneWatched;
    }

    /**
     * Sets whether this clause is a one literal watched clause or not.
     * @param oneWatched {@code true} if it is a one literal watched clause, {@code false} otherwise
     */
    public void setOneWatched(final boolean oneWatched) {
        this.oneWatched = oneWatched;
    }

    /**
     * Returns {@code true} if this is an at-most clause, {@code false} otherwise.
     * @return {@code true} if this is an at-most clause
     */
    public boolean isAtMost() {
        return isAtMost;
    }

    /**
     * Returns the number of watchers if this is an at-most clause.
     * @return the number of watchers
     */
    public int atMostWatchers() {
        assert isAtMost;
        return atMostWatchers;
    }

    /**
     * Sets the number of watchers for this at-most clause.
     * @param atMostWatchers the number of watchers
     */
    public void setAtMostWatchers(final int atMostWatchers) {
        assert isAtMost;
        this.atMostWatchers = atMostWatchers;
    }

    /**
     * Pops (removes) the last literal of this clause.
     */
    public void pop() {
        data.pop();
    }

    /**
     * Returns the right-hand k side of an at-most k constraint.
     * @return the right-hand side
     */
    public int cardinality() {
        return data.size() - atMostWatchers + 1;
    }

    /**
     * Returns the literals of this clause.
     * @return the literals of this clause
     */
    public LNGIntVector getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MSClause{");
        sb.append("activity=").append(activity).append(", ");
        sb.append("learntOnState=").append(learntOnState).append(", ");
        sb.append("seen=").append(seen).append(", ");
        sb.append("lbd=").append(lbd).append(", ");
        sb.append("canBeDel=").append(canBeDel).append(", ");
        sb.append("oneWatched=").append(oneWatched).append(", ");
        sb.append("isAtMost=").append(isAtMost).append(", ");
        sb.append("atMostWatchers=").append(atMostWatchers).append(", ");
        sb.append("lits=[");
        for (int i = 0; i < data.size(); i++) {
            final int lit = data.get(i);
            sb.append((lit & 1) == 1 ? "-" : "").append(lit >> 1);
            if (i != data.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]}");
        return sb.toString();
    }
}
