// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

import com.booleworks.logicng.collections.LngIntVector;

/**
 * A hard clause for the MaxSAT solver.
 * @version 1.0
 * @since 1.0
 */
public final class LngHardClause {

    private final LngIntVector clause;

    /**
     * Constructs a new hard clause.
     * @param clause the clause
     */
    public LngHardClause(final LngIntVector clause) {
        this.clause = new LngIntVector(clause);
    }

    /**
     * Returns the clause of this hard clause.
     * @return the clause
     */
    public LngIntVector clause() {
        return clause;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LngHardClause{lits=[");
        for (int i = 0; i < clause.size(); i++) {
            final int lit = clause.get(i);
            sb.append((lit & 1) == 1 ? "-" : "").append(lit >> 1);
            if (i != clause.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]}");
        return sb.toString();
    }
}
