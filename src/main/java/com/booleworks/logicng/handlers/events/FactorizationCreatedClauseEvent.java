package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.transformations.cnf.CNFFactorization;
import com.booleworks.logicng.transformations.dnf.DNFFactorization;

/**
 * An event created when a factorization algorithm (e.g.
 * {@link DNFFactorization} or {@link CNFFactorization})
 * creates a new clause.
 * @version 3.0.0
 * @since 3.0.0
 */
public class FactorizationCreatedClauseEvent implements LogicNGEvent {

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
