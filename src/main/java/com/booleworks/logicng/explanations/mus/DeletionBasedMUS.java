// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.Handler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SolverState;

import java.util.ArrayList;
import java.util.List;

/**
 * A naive deletion-based MUS algorithm.
 * @version 2.1.0
 * @since 1.1
 */
public final class DeletionBasedMUS extends MUSAlgorithm {

    @Override
    public <T extends Proposition> UNSATCore<T> computeMUS(final FormulaFactory f, final List<T> propositions, final MUSConfig config) {
        Handler.start(config.handler);
        final List<T> mus = new ArrayList<>(propositions.size());
        final List<SolverState> solverStates = new ArrayList<>(propositions.size());
        final MiniSat solver = MiniSat.miniSat(f);
        for (final Proposition proposition : propositions) {
            solverStates.add(solver.saveState());
            solver.add(proposition);
        }
        boolean sat = solver.satCall().handler(config.handler).sat() == Tristate.TRUE;
        if (Handler.aborted(config.handler)) {
            return null;
        }
        if (sat) {
            throw new IllegalArgumentException("Cannot compute a MUS for a satisfiable formula set.");
        }
        for (int i = solverStates.size() - 1; i >= 0; i--) {
            solver.loadState(solverStates.get(i));
            for (final Proposition prop : mus) {
                solver.add(prop);
            }
            sat = solver.satCall().handler(config.handler).sat() == Tristate.TRUE;
            if (Handler.aborted(config.handler)) {
                return null;
            }
            if (sat) {
                mus.add(propositions.get(i));
            }
        }
        return new UNSATCore<>(mus, true);
    }
}
