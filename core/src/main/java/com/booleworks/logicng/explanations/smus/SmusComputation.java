// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.smus;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SMUS_COMPUTATION_STARTED;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
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
     * satisfiable or the handler canceled the computation
     */
    public static <P extends Proposition> List<P> computeSmus(
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
     * satisfiable or the handler canceled the computation
     */
    public static <P extends Proposition> LNGResult<List<P>> computeSmus(
            final FormulaFactory f, final List<P> propositions,
            final List<Formula> additionalConstraints, final ComputationHandler handler) {
        if (!handler.shouldResume(SMUS_COMPUTATION_STARTED)) {
            return LNGResult.canceled(SMUS_COMPUTATION_STARTED);
        }
        final SATSolver growSolver = SATSolver.newSolver(f);
        growSolver.add(additionalConstraints == null ? Collections.singletonList(f.verum()) : additionalConstraints);
        final Map<Variable, P> propositionMapping = new TreeMap<>();
        for (final P proposition : propositions) {
            final Variable selector = f.variable(PROPOSITION_SELECTOR + propositionMapping.size());
            propositionMapping.put(selector, proposition);
            growSolver.add(f.equivalence(selector, proposition.getFormula()));
        }
        final LNGResult<Boolean> sat =
                growSolver.satCall().handler(handler).addFormulas(propositionMapping.keySet()).sat();
        if (!sat.isSuccess()) {
            return LNGResult.canceled(sat.getCancelCause());
        }
        final SATSolver hSolver = SATSolver.newSolver(f);
        while (true) {
            final LNGResult<SortedSet<Variable>> h = minimumHs(hSolver, propositionMapping.keySet(), handler);
            if (!h.isSuccess()) {
                return LNGResult.canceled(h.getCancelCause());
            } else {
                final SortedSet<Variable> hResult = h.getResult();
                final LNGResult<SortedSet<Variable>> c =
                        grow(growSolver, hResult, propositionMapping.keySet(), handler);
                if (c == null) {
                    return LNGResult.of(hResult.stream().map(propositionMapping::get).collect(Collectors.toList()));
                } else if (!c.isSuccess()) {
                    return LNGResult.canceled(c.getCancelCause());
                } else {
                    hSolver.add(f.or(c.getResult()));
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
     * satisfiable or the handler canceled the computation
     */
    public static List<Formula> computeSmusForFormulas(final FormulaFactory f, final List<Formula> formulas,
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
     * satisfiable or the handler canceled the computation
     */
    public static LNGResult<List<Formula>> computeSmusForFormulas(
            final FormulaFactory f, final List<Formula> formulas,
            final List<Formula> additionalConstraints, final ComputationHandler handler) {
        final List<Proposition> props = formulas.stream().map(StandardProposition::new).collect(Collectors.toList());
        final LNGResult<List<Proposition>> smus = computeSmus(f, props, additionalConstraints, handler);
        if (!smus.isSuccess()) {
            return LNGResult.canceled(smus.getCancelCause());
        } else {
            return LNGResult.of(smus.getResult().stream().map(Proposition::getFormula).collect(Collectors.toList()));
        }
    }

    private static LNGResult<SortedSet<Variable>> minimumHs(final SATSolver hSolver,
                                                            final Set<Variable> variables,
                                                            final ComputationHandler handler) {
        final LNGResult<Model> minimumHsModel = hSolver.execute(OptimizationFunction.builder()
                .literals(variables)
                .minimize().build(), handler);
        if (!minimumHsModel.isSuccess()) {
            return LNGResult.canceled(minimumHsModel.getCancelCause());
        } else {
            return LNGResult.of(minimumHsModel.getResult().positiveVariables());
        }
    }

    private static LNGResult<SortedSet<Variable>> grow(final SATSolver growSolver, final SortedSet<Variable> h,
                                                       final Set<Variable> variables,
                                                       final ComputationHandler handler) {
        final SolverState solverState = growSolver.saveState();
        growSolver.add(h);
        if (!growSolver.sat()) {
            return null;
        }
        final LNGResult<Model> maxModel = growSolver.execute(OptimizationFunction.builder()
                .literals(variables)
                .maximize().build(), handler);
        if (!maxModel.isSuccess()) {
            return LNGResult.canceled(maxModel.getCancelCause());
        } else {
            final SortedSet<Variable> maximumSatisfiableSet = maxModel.getResult().positiveVariables();
            growSolver.loadState(solverState);
            final SortedSet<Variable> minimumCorrectionSet = new TreeSet<>(variables);
            maximumSatisfiableSet.forEach(minimumCorrectionSet::remove);
            return LNGResult.of(minimumCorrectionSet);
        }
    }
}