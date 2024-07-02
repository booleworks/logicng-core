// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MUS_COMPUTATION_STARTED;

import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.ArrayList;
import java.util.List;

/**
 * A naive plain insertion-based MUS algorithm.
 * @version 2.1.0
 * @since 1.1
 */
public class PlainInsertionBasedMUS extends MUSAlgorithm {

    @Override
    public <T extends Proposition> LNGResult<UNSATCore<T>> computeMUS(
            final FormulaFactory f, final List<T> propositions,
            final MUSConfig config, final ComputationHandler handler) {
        if (!handler.shouldResume(MUS_COMPUTATION_STARTED)) {
            return LNGResult.aborted(MUS_COMPUTATION_STARTED);
        }
        final List<T> currentFormula = new ArrayList<>(propositions.size());
        currentFormula.addAll(propositions);
        final List<T> mus = new ArrayList<>(propositions.size());
        while (!currentFormula.isEmpty()) {
            final List<T> currentSubset = new ArrayList<>(propositions.size());
            T transitionProposition = null;
            final SATSolver solver = SATSolver.newSolver(f);
            for (final Proposition p : mus) {
                solver.add(p);
            }
            int count = currentFormula.size();
            while (true) {
                final LNGResult<Boolean> sat = solver.satCall().handler(handler).sat();
                if (!sat.isSuccess()) {
                    return LNGResult.aborted(sat.getAbortionEvent());
                }
                if (!sat.getResult()) {
                    break;
                }
                if (count == 0) {
                    throw new IllegalArgumentException("Cannot compute a MUS for a satisfiable formula set.");
                }
                final T removeProposition = currentFormula.get(--count);
                currentSubset.add(removeProposition);
                transitionProposition = removeProposition;
                solver.add(removeProposition);
            }
            currentFormula.clear();
            currentFormula.addAll(currentSubset);
            if (transitionProposition != null) {
                currentFormula.remove(transitionProposition);
                mus.add(transitionProposition);
            }
        }
        return LNGResult.of(new UNSATCore<>(mus, true));
    }

}
