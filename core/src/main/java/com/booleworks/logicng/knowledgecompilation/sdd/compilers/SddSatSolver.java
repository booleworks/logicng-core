package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfSatSolver;

import java.util.BitSet;

/**
 * Interface for SDD Sat Solver
 * @version 3.0.0
 * @since 3.0.0
 */
public interface SddSatSolver extends DnnfSatSolver {
    /**
     * Checks whether a literal of the variable is implied by the solver's state and returns it, otherwise it returns
     * {@code -1}.
     * @return an implied literal of the variable in the current solver state or {@code -1} if not implied at all
     */
    int impliedLiteral(int variable);

    /**
     * Returns the set of all literals implied by the solver's state.
     * @return the set of all literals implied by the solver's state
     */
    BitSet impliedLiteralBitset();

    /**
     * Returns the set of all clauses subsumed by the solver's state.
     * @return the set of all clauses subsumed by the solver's state
     */
    BitSet subsumedClauseBitset();
}
