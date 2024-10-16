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
 * A soft clause for the MaxSAT solver.
 * @version 1.0
 * @since 1.0
 */
public final class LngSoftClause {

    private final LngIntVector clause;
    private final LngIntVector relaxationVars;
    private int weight;
    private int assumptionVar;

    /**
     * Constructs a new soft clause.
     * @param clause         the clause
     * @param weight         the weight of this clause
     * @param assumptionVar  the assumption variables of this clause
     * @param relaxationVars the relaxation variables
     */
    public LngSoftClause(final LngIntVector clause, final int weight, final int assumptionVar,
                         final LngIntVector relaxationVars) {
        this.clause = new LngIntVector(clause);
        this.weight = weight;
        this.assumptionVar = assumptionVar;
        this.relaxationVars = new LngIntVector(relaxationVars);
    }

    /**
     * Returns the clause of this soft clause.
     * @return the clause
     */
    public LngIntVector clause() {
        return clause;
    }

    /**
     * Returns the weight of this soft clause.
     * @return the weight
     */
    public int weight() {
        return weight;
    }

    /**
     * Sets the weight
     * @param weight the weight
     */
    public void setWeight(final int weight) {
        this.weight = weight;
    }

    /**
     * Returns the relaxation variables of this soft clause.
     * @return the relaxation variables
     */
    public LngIntVector relaxationVars() {
        return relaxationVars;
    }

    /**
     * Returns the assumption variable.
     * @return the assumption variable
     */
    public int assumptionVar() {
        return assumptionVar;
    }

    /**
     * Sets the assumption variable.
     * @param assumptionVar the assumption variable
     */
    public void setAssumptionVar(final int assumptionVar) {
        this.assumptionVar = assumptionVar;
    }

    @Override
    public String toString() {
        final StringBuilder sb =
                new StringBuilder(String.format("LngSoftClause{weight=%d, assumption=%d lits=[", weight, assumptionVar));
        for (int i = 0; i < clause.size(); i++) {
            final int lit = clause.get(i);
            sb.append((lit & 1) == 1 ? "-" : "").append(lit >> 1);
            if (i != clause.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("] relax[");
        for (int i = 0; i < relaxationVars.size(); i++) {
            final int lit = relaxationVars.get(i);
            sb.append((lit & 1) == 1 ? "-" : "").append(lit >> 1);
            if (i != relaxationVars.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]}");
        return sb.toString();
    }
}
