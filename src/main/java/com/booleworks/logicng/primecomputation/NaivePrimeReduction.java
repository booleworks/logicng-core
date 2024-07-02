// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.util.FormulaHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Naive implementation for reducing implicants and implicates to prime ones.
 * <p>
 * The computation is initialized with the formula for which the prime
 * implicants/implicates should be computed.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class NaivePrimeReduction {

    private final SATSolver implicantSolver;
    private final SATSolver implicateSolver;

    /**
     * Creates a new prime implicant computation for a given formula.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     */
    public NaivePrimeReduction(final FormulaFactory f, final Formula formula) {
        implicantSolver = SATSolver.newSolver(f,
                SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        implicantSolver.add(formula.negate(f));
        implicateSolver = SATSolver.newSolver(f,
                SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        implicateSolver.add(formula);
    }

    /**
     * Computes a prime implicant from the given implicant for the given
     * formula. Assumption: Given implicant is a satisfying assignment for the
     * formula
     * @param implicant the implicant
     * @return a prime implicant
     */
    public SortedSet<Literal> reduceImplicant(final SortedSet<Literal> implicant) {
        return reduceImplicant(implicant, NopHandler.get()).getResult();
    }

    /**
     * Computes a prime implicant from the given implicant for the given
     * formula. Assumption: Given implicant is a satisfying assignment for the
     * formula
     * @param implicant the implicant
     * @param handler   a SAT handler for the underlying SAT Solver
     * @return a prime implicant or null if the computation was aborted by the
     *         handler
     */
    public LNGResult<SortedSet<Literal>> reduceImplicant(final SortedSet<Literal> implicant,
                                                         final ComputationHandler handler) {
        handler.shouldResume(ComputationStartedEvent.IMPLICATE_REDUCTION_STARTED);
        final SortedSet<Literal> primeImplicant = new TreeSet<>(implicant);
        for (final Literal lit : implicant) {
            primeImplicant.remove(lit);
            final LNGResult<Boolean> sat =
                    implicantSolver.satCall().handler(handler).addFormulas(primeImplicant).sat();
            if (!sat.isSuccess()) {
                return LNGResult.aborted(sat.getAbortionEvent());
            }
            if (sat.getResult()) {
                primeImplicant.add(lit);
            }
        }
        return LNGResult.of(primeImplicant);
    }

    /**
     * Computes a prime implicate from the given implicate for the given
     * formula. Assumption: Given implicate is a falsifying assignment for the
     * formula, i.e. a satisfying assignment for the negated formula
     * @param f         the formula factory to generate new formulas
     * @param implicate the implicate
     * @return a prime implicate
     */
    public SortedSet<Literal> reduceImplicate(final FormulaFactory f, final SortedSet<Literal> implicate) {
        return reduceImplicate(f, implicate, NopHandler.get()).getResult();
    }

    /**
     * Computes a prime implicate from the given implicate for the given
     * formula. Assumption: Given implicate is a falsifying assignment for the
     * formula, i.e. a satisfying assignment for the negated formula
     * @param f         the formula factory to generate new formulas
     * @param implicate the implicate
     * @param handler   a SAT handler for the underlying SAT Solver
     * @return a prime implicate of null if the computation was aborted by the
     *         handler
     */
    public LNGResult<SortedSet<Literal>> reduceImplicate(final FormulaFactory f, final SortedSet<Literal> implicate,
                                                         final ComputationHandler handler) {
        handler.shouldResume(ComputationStartedEvent.IMPLICATE_REDUCTION_STARTED);
        final SortedSet<Literal> primeImplicate = new TreeSet<>(implicate);
        for (final Literal lit : implicate) {
            primeImplicate.remove(lit);
            final List<Literal> assumptions = FormulaHelper.negateLiterals(f, primeImplicate, ArrayList::new);
            final LNGResult<Boolean> sat = implicateSolver.satCall().handler(handler).addFormulas(assumptions).sat();
            if (!sat.isSuccess()) {
                return LNGResult.aborted(sat.getAbortionEvent());
            }
            if (sat.getResult()) {
                primeImplicate.add(lit);
            }
        }
        return LNGResult.of(primeImplicate);
    }
}
