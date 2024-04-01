// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * A solver function which returns all unit propagated literals on level 0 of the current
 * formula on the solver.  If the formula is UNSAT, {@code null} will be returned.
 * @version 2.0.0
 * @since 2.0.0
 */
public final class UpZeroLiteralsFunction implements SolverFunction<SortedSet<Literal>> {

    private final static UpZeroLiteralsFunction INSTANCE = new UpZeroLiteralsFunction();

    /**
     * Private empty constructor.  Singleton class.
     */
    private UpZeroLiteralsFunction() {
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
    public SortedSet<Literal> apply(final SATSolver solver, final Consumer<Tristate> resultSetter) {
        if (!solver.sat()) {
            return null;
        }
        final LNGIntVector literals = solver.underlyingSolver().upZeroLiterals();
        final SortedSet<Literal> upZeroLiterals = new TreeSet<>();
        for (int i = 0; i < literals.size(); ++i) {
            final int lit = literals.get(i);
            upZeroLiterals.add(solver.factory().literal(solver.underlyingSolver().nameForIdx(LNGCoreSolver.var(lit)), !LNGCoreSolver.sign(lit)));
        }
        return upZeroLiterals;
    }
}
