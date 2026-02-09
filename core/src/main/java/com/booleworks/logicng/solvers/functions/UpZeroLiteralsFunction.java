// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A solver function which returns all unit propagated literals on level 0 of
 * the current formula on the solver. If the formula is UNSAT, {@code null} will
 * be returned.
 * @version 3.0.0
 * @since 2.0.0
 */
public class UpZeroLiteralsFunction implements SolverFunction<SortedSet<Literal>> {

    private final static UpZeroLiteralsFunction INSTANCE = new UpZeroLiteralsFunction();

    protected UpZeroLiteralsFunction() {
        // Intentionally left empty
    }

    /**
     * Returns the singleton of the function.
     * @return the function instance
     */
    public static UpZeroLiteralsFunction get() {
        return INSTANCE;
    }

    @Override
    public LngResult<SortedSet<Literal>> apply(final SatSolver solver, final ComputationHandler handler) {
        if (!solver.sat()) {
            return LngResult.of(new TreeSet<>());
        }
        final LngIntVector literals = solver.getUnderlyingSolver().upZeroLiterals();
        final SortedSet<Literal> upZeroLiterals = new TreeSet<>();
        for (int i = 0; i < literals.size(); ++i) {
            final int lit = literals.get(i);
            upZeroLiterals.add(solver.getFactory().literal(solver.getUnderlyingSolver().nameForIdx(LngCoreSolver.var(lit)),
                    !LngCoreSolver.sign(lit)));
        }
        return LngResult.of(upZeroLiterals);
    }
}
