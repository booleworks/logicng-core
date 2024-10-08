// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Open-WBO -- Copyright (c) 2013-2015, Ruben Martins, Vasco Manquinho, Ines
 * Lynce <p> Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions: <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. <p> THE SOFTWARE IS
 * PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
