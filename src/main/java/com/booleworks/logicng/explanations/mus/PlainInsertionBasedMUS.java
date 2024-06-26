// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MUS_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.NO_EVENT;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
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
    public <T extends Proposition> UNSATCore<T> computeMUS(final FormulaFactory f, final List<T> propositions,
                                                           final MUSConfig config) {
        config.handler.shouldResume(MUS_COMPUTATION_STARTED);
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
            while (shouldProceed(solver, config.handler)) {
                if (count == 0) {
                    throw new IllegalArgumentException("Cannot compute a MUS for a satisfiable formula set.");
                }
                final T removeProposition = currentFormula.get(--count);
                currentSubset.add(removeProposition);
                transitionProposition = removeProposition;
                solver.add(removeProposition);
            }
            if (!config.handler.shouldResume(NO_EVENT)) {
                return null;
            }
            currentFormula.clear();
            currentFormula.addAll(currentSubset);
            if (transitionProposition != null) {
                currentFormula.remove(transitionProposition);
                mus.add(transitionProposition);
            }
        }
        return new UNSATCore<>(mus, true);
    }

    private static boolean shouldProceed(final SATSolver solver, final ComputationHandler handler) {
        final boolean sat = solver.satCall().handler(handler).sat() == Tristate.TRUE;
        return sat && handler.shouldResume(NO_EVENT);
    }
}
