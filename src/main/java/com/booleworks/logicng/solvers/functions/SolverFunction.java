// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import com.booleworks.logicng.solvers.SATSolver;

/**
 * An interface for a function which works on a given SAT solver and its state.
 * <p>
 * With the help of solver functions, additional methods can be plugged to SAT
 * solver without extending the solver classes themselves.
 * @param <RESULT> the result type of the function
 * @version 2.0.0
 * @since 2.0.0
 */
public interface SolverFunction<RESULT> {

    /**
     * Applies this function to the given solver.
     * @param solver the solver on which the function should work on
     * @return the result of the function application
     */
    RESULT apply(SATSolver solver);
}
