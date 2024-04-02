// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.smus;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.Handler;
import com.booleworks.logicng.handlers.OptimizationHandler;
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
 * Implementation is based on &quot;Smallest MUS extraction with minimal
 * hitting set dualization&quot; (Ignatiev, Previti, Liffiton, &amp;
 * Marques-Silva, 2015).
 * @version 2.1.0
 * @since 2.0.0
 */
public final class SmusComputation {

    private static final String PROPOSITION_SELECTOR = "@PROPOSITION_SEL_";

    /**
     * Private empty constructor.  Class only contains static utility methods.
     */
    private SmusComputation() {
        // Intentionally left empty
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some additional constraint.
     * @param <P>                   the subtype of the propositions
     * @param f                     the formula factory
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> List<P> computeSmus(final FormulaFactory f, final List<P> propositions, final List<Formula> additionalConstraints) {
        return computeSmus(f, propositions, additionalConstraints, null);
    }

    /**
     * Computes the SMUS for the given list of propositions modulo some additional constraint.
     * <p>
     * The SMUS computation can be called with an {@link OptimizationHandler}. The given handler instance will be used for every subsequent
     * * {@link OptimizationFunction} call and the handler's SAT handler is used for every subsequent SAT call.
     * @param <P>                   the subtype of the propositions
     * @param f                     the formula factory
     * @param propositions          the propositions
     * @param additionalConstraints the additional constraints
     * @param handler               the handler, can be {@code null}
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static <P extends Proposition> List<P> computeSmus(final FormulaFactory f, final List<P> propositions, final List<Formula> additionalConstraints,
                                                              final OptimizationHandler handler) {
        Handler.start(handler);
        final SATSolver growSolver = SATSolver.newSolver(f);
        growSolver.add(additionalConstraints == null ? Collections.singletonList(f.verum()) : additionalConstraints);
        final Map<Variable, P> propositionMapping = new TreeMap<>();
        for (final P proposition : propositions) {
            final Variable selector = f.variable(PROPOSITION_SELECTOR + propositionMapping.size());
            propositionMapping.put(selector, proposition);
            growSolver.add(f.equivalence(selector, proposition.formula()));
        }
        final boolean sat = growSolver.satCall().handler(OptimizationHandler.satHandler(handler)).addFormulas(propositionMapping.keySet()).sat() == Tristate.TRUE;
        if (sat || Handler.aborted(handler)) {
            return null;
        }
        final SATSolver hSolver = SATSolver.newSolver(f);
        while (true) {
            final SortedSet<Variable> h = minimumHs(hSolver, propositionMapping.keySet(), handler);
            if (h == null || Handler.aborted(handler)) {
                return null;
            }
            final SortedSet<Variable> c = grow(growSolver, h, propositionMapping.keySet(), handler);
            if (Handler.aborted(handler)) {
                return null;
            }
            if (c == null) {
                return h.stream().map(propositionMapping::get).collect(Collectors.toList());
            }
            hSolver.add(f.or(c));
        }
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional constraints.
     * @param f                     the formula factory
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static List<Formula> computeSmusForFormulas(final FormulaFactory f, final List<Formula> formulas, final List<Formula> additionalConstraints) {
        return computeSmusForFormulas(f, formulas, additionalConstraints, null);
    }

    /**
     * Computes the SMUS for the given list of formulas and some additional constraints.
     * @param f                     the formula factory
     * @param formulas              the formulas
     * @param additionalConstraints the additional constraints
     * @param handler               the SMUS handler, can be {@code null}
     * @return the SMUS or {@code null} if the given propositions are satisfiable or the handler aborted the computation
     */
    public static List<Formula> computeSmusForFormulas(final FormulaFactory f, final List<Formula> formulas, final List<Formula> additionalConstraints,
                                                       final OptimizationHandler handler) {
        final List<Proposition> props = formulas.stream().map(StandardProposition::new).collect(Collectors.toList());
        final List<Proposition> smus = computeSmus(f, props, additionalConstraints, handler);
        return smus == null ? null : smus.stream().map(Proposition::formula).collect(Collectors.toList());
    }

    private static SortedSet<Variable> minimumHs(final SATSolver hSolver, final Set<Variable> variables, final OptimizationHandler handler) {
        final Assignment minimumHsModel = hSolver.execute(OptimizationFunction.builder()
                .handler(handler)
                .literals(variables)
                .minimize().build());
        return Handler.aborted(handler) ? null : new TreeSet<>(minimumHsModel.positiveVariables());
    }

    private static SortedSet<Variable> grow(final SATSolver growSolver, final SortedSet<Variable> h, final Set<Variable> variables, final OptimizationHandler handler) {
        final SolverState solverState = growSolver.saveState();
        growSolver.add(h);
        final Assignment maxModel = growSolver.execute(OptimizationFunction.builder()
                .handler(handler)
                .literals(variables)
                .maximize().build());
        if (maxModel == null || Handler.aborted(handler)) {
            return null;
        } else {
            final SortedSet<Variable> maximumSatisfiableSet = maxModel.positiveVariables();
            growSolver.loadState(solverState);
            final SortedSet<Variable> minimumCorrectionSet = new TreeSet<>(variables);
            maximumSatisfiableSet.forEach(minimumCorrectionSet::remove);
            return minimumCorrectionSet;
        }
    }
}
