// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.transformations.cnf.CnfFactorization;
import com.booleworks.logicng.transformations.dnf.DnfFactorization;

/**
 * An event created when a factorization algorithm (e.g.
 * {@link DnfFactorization} or {@link CnfFactorization})
 * creates a new clause.
 * @version 3.0.0
 * @since 3.0.0
 */
public class FactorizationCreatedClauseEvent implements LngEvent {

    private final Formula clause;

    /**
     * Creates a new event with the clause which was created.
     * @param clause the clause
     */
    public FactorizationCreatedClauseEvent(final Formula clause) {
        this.clause = clause;
    }

    /**
     * Returns the created clause.
     * @return the created clause
     */
    public Formula getClause() {
        return clause;
    }

    @Override
    public String toString() {
        return "Event: Created clause during factorization: " + clause;
    }
}
