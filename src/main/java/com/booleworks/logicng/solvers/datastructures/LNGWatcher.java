// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

/**
 * A watcher for clauses for SAT solvers.
 * @version 1.0.1
 * @since 1.0
 */
public final class LNGWatcher {
    private final LNGClause clause;
    private final int blocker;

    /**
     * Constructs a new watcher.
     * @param clause  the watched clause
     * @param blocker the blocking literal
     */
    public LNGWatcher(final LNGClause clause, final int blocker) {
        this.clause = clause;
        this.blocker = blocker;
    }

    /**
     * Returns the blocking literal of this watcher.
     * @return the blocking literal of this watcher
     */
    public int blocker() {
        return blocker;
    }

    /**
     * Returns the watched clause of this watcher.
     * @return the watched clause of this watcher
     */
    public LNGClause clause() {
        return clause;
    }

    @Override
    public int hashCode() {
        return clause.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof LNGWatcher && clause == (((LNGWatcher) other).clause);
    }

    @Override
    public String toString() {
        return String.format("MSWatcher{clause=%s, blocker=%d}", clause, blocker);
    }
}
