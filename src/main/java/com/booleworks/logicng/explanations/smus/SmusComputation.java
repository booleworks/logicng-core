// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.smus;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SMUS_COMPUTATION_STARTED;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.SatResult;
import com.booleworks.logicng.handlers.UnsatResult;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Computation of a SMUS (smallest MUS, smallest minimal unsatisfiable set).
 * <p>
 * Implementation is based on &quot;Smallest MUS extraction with minimal hitting
 * set dualization&quot; (Ignatiev, Previti, Liffiton, &amp; Marques-Silva,
 * 2015).
 * @version 2.1.0
 * @since 2.0.0
 */
public final class SmusComputation {

    private static final String PROPOSITION_SELECTOR = "@PROPOSITION_SEL_";

    /**
     * Private empty constructor. Class only contains static utility methods.
     */
    private SmusComputation() {
        // Intentionally left empty
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some
     * additional constraint.
     * @param <P>                   the subtype of the propositions
     * @param f                     the formula factory
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @return the SMUS or {@code null} if the given propositions are
     *         satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> UnsatResult<List<P>> computeSmus(
            final FormulaFactory f, final List<P> propositions,
            final List<Formula> additionalConstraints) {
        return computeSmus(f, propositions, additionalConstraints, NopHandler.get()).getResult();
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some
     * additional constraint.
     * <p>
     * The SMUS computation can be called with an {@link ComputationHandler}.
     * The given handler instance will be used for every subsequent *
     * {@link OptimizationFunction} call and the handler's SAT handler is used
     * for every subsequent SAT call.
     * @param <P>                   the subtype of the propositions
     * @param f                     the formula factory
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @param handler               the handler, can be {@code null}
     * @return the SMUS or {@code null} if the given propositions are
     *         satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> LNGResult<UnsatResult<List<P>>> computeSmus(
            final FormulaFactory f, final List<P> propositions,
            final List<Formula> additionalConstraints, final ComputationHandler handler) {
        if (!handler.shouldResume(SMUS_COMPUTATION_STARTED)) {
            return LNGResult.aborted(SMUS_COMPUTATION_STARTED);
        }
        final SATSolver growSolver = SATSolver.newSolver(f);
        growSolver.add(additionalConstraints == null ? Collections.singletonList(f.verum()) : additionalConstraints);
        final Map<Variable, P> propositionMapping = new TreeMap<>();
        for (final P proposition : propositions) {
            final Variable selector = f.variable(PROPOSITION_SELECTOR + propositionMapping.size());
            propositionMapping.put(selector, proposition);
            growSolver.add(f.equivalence(selector, proposition.formula()));
        }
        final LNGResult<Boolean> sat =
                growSolver.satCall().handler(handler).addFormulas(propositionMapping.keySet()).sat();
        if (!sat.isSuccess()) {
            return LNGResult.aborted(sat.getAbortionEvent());
        } else if (sat.getResult()) {
            return LNGResult.of(UnsatResult.sat());
        }
        final SATSolver hSolver = SATSolver.newSolver(f);
        while (true) {
            final LNGResult<SatResult<SortedSet<Variable>>> h =
                    minimumHs(hSolver, propositionMapping.keySet(), handler);
            if (!h.isSuccess()) {
                return LNGResult.aborted(h.getAbortionEvent());
            } else if (!h.getResult().isSat()) {
                return LNGResult.of(UnsatResult.sat());
            } else {
                final SortedSet<Variable> hResult = h.getResult().getResult();
                final LNGResult<SatResult<SortedSet<Variable>>> c =
                        grow(growSolver, hResult, propositionMapping.keySet(), handler);
                if (!c.isSuccess()) {
                    return LNGResult.aborted(c.getAbortionEvent());
                } else if (!c.getResult().isSat()) {
                    return LNGResult.of(UnsatResult.unsat(
                            hResult.stream().map(propositionMapping::get).collect(Collectors.toList())));
                } else {
                    hSolver.add(f.or(c.getResult().getResult()));
                }
            }
        }
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional
     * constraints.
     * @param f                     the formula factory
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @return the SMUS or {@code null} if the given propositions are
     *         satisfiable or the handler aborted the computation
     */
    public static UnsatResult<List<Formula>> computeSmusForFormulas(final FormulaFactory f, final List<Formula> formulas,
                                                                    final List<Formula> additionalConstraints) {
        return computeSmusForFormulas(f, formulas, additionalConstraints, NopHandler.get()).getResult();
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional
     * constraints.
     * @param f                     the formula factory
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @param handler               the SMUS handler, can be {@code null}
     * @return the SMUS or {@code null} if the given propositions are
     *         satisfiable or the handler aborted the computation
     */
    public static LNGResult<UnsatResult<List<Formula>>> computeSmusForFormulas(
            final FormulaFactory f, final List<Formula> formulas,
            final List<Formula> additionalConstraints, final ComputationHandler handler) {
        final List<Proposition> props = formulas.stream().map(StandardProposition::new).collect(Collectors.toList());
        final LNGResult<UnsatResult<List<Proposition>>> smus = computeSmus(f, props, additionalConstraints, handler);
        if (!smus.isSuccess()) {
            return LNGResult.aborted(smus.getAbortionEvent());
        } else if (!smus.getResult().isUnsat()) {
            return LNGResult.of(UnsatResult.sat());
        } else {
            return LNGResult.of(UnsatResult.unsat(smus.getResult().getResult().stream()
                    .map(Proposition::formula).collect(Collectors.toList())));
        }
    }

    private static LNGResult<SatResult<SortedSet<Variable>>> minimumHs(final SATSolver hSolver,
                                                                       final Set<Variable> variables,
                                                                       final ComputationHandler handler) {
        final LNGResult<SatResult<Assignment>> minimumHsModel = hSolver.execute(OptimizationFunction.builder()
                .literals(variables)
                .minimize().build(), handler);
        if (!minimumHsModel.isSuccess()) {
            return LNGResult.aborted(minimumHsModel.getAbortionEvent());
        } else if (minimumHsModel.getResult().isSat()) {
            final SortedSet<Variable> model = minimumHsModel.getResult().getResult().positiveVariables();
            return LNGResult.of(SatResult.sat(new TreeSet<>(model)));
        } else {
            return LNGResult.of(SatResult.unsat());
        }
    }

    private static LNGResult<SatResult<SortedSet<Variable>>> grow(
            final SATSolver growSolver, final SortedSet<Variable> h,
            final Set<Variable> variables, final ComputationHandler handler) {
        final SolverState solverState = growSolver.saveState();
        growSolver.add(h);
        final LNGResult<SatResult<Assignment>> maxModel = growSolver.execute(OptimizationFunction.builder()
                .literals(variables)
                .maximize().build(), handler);
        if (!maxModel.isSuccess()) {
            return LNGResult.aborted(maxModel.getAbortionEvent());
        } else if (!maxModel.getResult().isSat()) {
            return LNGResult.of(SatResult.unsat());
        } else {
            final SortedSet<Variable> maximumSatisfiableSet = maxModel.getResult().getResult().positiveVariables();
            growSolver.loadState(solverState);
            final SortedSet<Variable> minimumCorrectionSet = new TreeSet<>(variables);
            maximumSatisfiableSet.forEach(minimumCorrectionSet::remove);
            return LNGResult.of(SatResult.sat(minimumCorrectionSet));
        }
    }
}
