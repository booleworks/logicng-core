// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MUS_COMPUTATION_STARTED;

import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;

import java.util.ArrayList;
import java.util.List;

/**
 * A naive deletion-based MUS algorithm.
 * @version 3.0.0
 * @since 1.1
 */
public final class DeletionBasedMus extends MusAlgorithm {

    @Override
    public <T extends Proposition> LngResult<UnsatCore<T>> computeMus(
            final FormulaFactory f, final List<T> propositions,
            final MusConfig config, final ComputationHandler handler) {
        if (!handler.shouldResume(MUS_COMPUTATION_STARTED)) {
            return LngResult.canceled(MUS_COMPUTATION_STARTED);
        }
        final List<T> mus = new ArrayList<>(propositions.size());
        final List<SolverState> solverStates = new ArrayList<>(propositions.size());
        final SatSolver solver = SatSolver.newSolver(f);
        for (final Proposition proposition : propositions) {
            solverStates.add(solver.saveState());
            solver.add(proposition);
        }
        LngResult<Boolean> sat = solver.satCall().handler(handler).sat();
        if (!sat.isSuccess()) {
            return LngResult.canceled(sat.getCancelCause());
        }
        if (sat.getResult()) {
            throw new IllegalArgumentException("Cannot compute a MUS for a satisfiable formula set.");
        }
        for (int i = solverStates.size() - 1; i >= 0; i--) {
            solver.loadState(solverStates.get(i));
            for (final Proposition prop : mus) {
                solver.add(prop);
            }
            sat = solver.satCall().handler(handler).sat();
            if (!sat.isSuccess()) {
                return LngResult.canceled(sat.getCancelCause());
            }
            if (sat.getResult()) {
                mus.add(propositions.get(i));
            }
        }
        return LngResult.of(new UnsatCore<>(mus, true));
    }
}
