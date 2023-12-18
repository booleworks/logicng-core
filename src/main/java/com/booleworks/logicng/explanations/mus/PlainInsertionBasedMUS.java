// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.Handler;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.MiniSat;
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
        Handler.start(config.handler);
        final List<T> currentFormula = new ArrayList<>(propositions.size());
        currentFormula.addAll(propositions);
        final List<T> mus = new ArrayList<>(propositions.size());
        final MiniSat solver = MiniSat.miniSat(f);
        while (!currentFormula.isEmpty()) {
            final List<T> currentSubset = new ArrayList<>(propositions.size());
            T transitionProposition = null;
            solver.reset();
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
            if (Handler.aborted(config.handler)) {
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

    private static boolean shouldProceed(final SATSolver solver, final SATHandler handler) {
        final boolean sat = solver.sat(handler) == Tristate.TRUE;
        return sat && !Handler.aborted(handler);
    }
}
