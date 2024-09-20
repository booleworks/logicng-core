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
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatState;

import java.util.ArrayList;
import java.util.Collection;
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
 * @version 3.0.0
 * @since 2.0.0
 */
public final class Smus {

    private static final String PROPOSITION_SELECTOR = "@PROP_SEL_";

    private final FormulaFactory f;
    final List<Formula> additionalConstraints;
    final MaxSatConfig maxSatConfig;

    private Smus(
            final FormulaFactory f,
            final List<Formula> additionalConstraints,
            final MaxSatConfig maxSatConfig) {
        this.f = f;
        this.additionalConstraints = additionalConstraints;
        this.maxSatConfig = maxSatConfig;
    }

    /**
     * Returns the builder for this computation.
     * @param f the formula factory
     * @return the builder
     */
    public static Builder builder(final FormulaFactory f) {
        return new Builder(f);
    }

    /**
     * Computes the SMUS for the given list of formulas.
     * @param formulas the formulas
     * @return the SMUS
     * @throws IllegalArgumentException if the formulas are satisfiable
     */
    public List<Formula> computeForFormulas(final List<Formula> formulas) {
        return computeForFormulas(formulas, NopHandler.get()).getResult();
    }

    /**
     * Computes the SMUS for the given list of formulas and a handler.
     * @param formulas the formulas
     * @param handler  the handler
     * @return the computation result containing the SMUS or a cancelation reason
     * @throws IllegalArgumentException if the formulas are satisfiable
     */
    public LngResult<List<Formula>> computeForFormulas(
            final List<Formula> formulas,
            final ComputationHandler handler) {
        final List<Proposition> props = formulas.stream().map(StandardProposition::new).collect(Collectors.toList());
        final LngResult<List<Proposition>> res = compute(props, handler);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        } else {
            return LngResult.of(res.getResult().stream().map(Proposition::getFormula).collect(Collectors.toList()));
        }
    }


    /**
     * Computes the SMUS for the given list of propositions.
     * @param <P>          the subtype of the propositions
     * @param propositions the propositions
     * @return the SMUS
     * @throws IllegalArgumentException if the propositions are satisfiable
     */
    public <P extends Proposition> List<P> compute(final List<P> propositions) {
        return compute(propositions, NopHandler.get()).getResult();
    }

    /**
     * Computes the SMUS for the given list of propositions and a handler.
     * @param <P>          the subtype of the propositions
     * @param propositions the propositions
     * @param handler      the handler
     * @return the computation result containing the SMUS or a cancelation reason
     * @throws IllegalArgumentException if the propositions are satisfiable
     */
    public <P extends Proposition> LngResult<List<P>> compute(
            final List<P> propositions,
            final ComputationHandler handler) {
        if (!handler.shouldResume(SMUS_COMPUTATION_STARTED)) {
            return LngResult.canceled(SMUS_COMPUTATION_STARTED);
        }
        final MaxSatSolver growSolver = MaxSatSolver.newSolver(f, maxSatConfig);
        additionalConstraints.forEach(growSolver::addHardFormula);
        final Map<Variable, P> propositionMapping = createPropositionsMapping(growSolver, propositions);

        final LngResult<MaxSatResult> sat = checkSat(growSolver, propositionMapping.keySet(), handler);
        if (!sat.isSuccess()) {
            return LngResult.canceled(sat.getCancelCause());
        }

        final MaxSatSolver hSolver = MaxSatSolver.newSolver(f, maxSatConfig);
        while (true) {
            final LngResult<SortedSet<Variable>> h = minimumHs(hSolver, propositionMapping.keySet(), handler);
            if (!h.isSuccess()) {
                return LngResult.canceled(h.getCancelCause());
            } else {
                final SortedSet<Variable> hResult = h.getResult();
                final LngResult<SortedSet<Variable>> c = grow(growSolver, hResult, propositionMapping.keySet(), handler);
                if (c == null) {
                    return LngResult.of(hResult.stream().map(propositionMapping::get).collect(Collectors.toList()));
                } else if (!c.isSuccess()) {
                    return LngResult.canceled(c.getCancelCause());
                } else {
                    hSolver.addHardFormula(f.or(c.getResult()));
                }
            }
        }
    }

    private <P extends Proposition> Map<Variable, P> createPropositionsMapping(
            final MaxSatSolver growSolver,
            final List<P> propositions) {
        final Map<Variable, P> propositionMapping = new TreeMap<>();
        for (final P proposition : propositions) {
            final Variable selector = f.variable(PROPOSITION_SELECTOR + propositionMapping.size());
            propositionMapping.put(selector, proposition);
            growSolver.addHardFormula(f.equivalence(selector, proposition.getFormula()));
        }
        return propositionMapping;
    }

    private LngResult<MaxSatResult> checkSat(
            final MaxSatSolver growSolver,
            final Set<Variable> variables,
            final ComputationHandler handler) {
        final MaxSatState state = growSolver.saveState();
        for (final Variable v : variables) {
            growSolver.addHardFormula(v);
        }
        final LngResult<MaxSatResult> res = growSolver.solve(handler);
        if (res.isSuccess() && res.getResult().isSatisfiable()) {
            throw new IllegalArgumentException("Formulas for SMUS generation are satisfiable");
        }
        growSolver.loadState(state);
        return res;
    }

    private LngResult<SortedSet<Variable>> minimumHs(
            final MaxSatSolver hSolver,
            final Set<Variable> variables,
            final ComputationHandler handler) {
        for (final Variable v : variables) {
            hSolver.addSoftFormula(v.negate(hSolver.getFactory()), 1);
        }
        final LngResult<MaxSatResult> res = hSolver.solve(handler);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        }
        final Model minimumHsModel = res.getResult().getModel();
        return LngResult.of(minimumHsModel.positiveVariables());
    }

    private LngResult<SortedSet<Variable>> grow(
            final MaxSatSolver growSolver,
            final SortedSet<Variable> h,
            final Set<Variable> variables,
            final ComputationHandler handler) {
        final MaxSatState solverState = growSolver.saveState();
        for (final Variable hVar : h) {
            growSolver.addHardFormula(hVar);
        }
        for (final Variable v : variables) {
            growSolver.addSoftFormula(v, 1);
        }
        final LngResult<MaxSatResult> res = growSolver.solve(handler);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        }
        if (!res.getResult().isSatisfiable()) {
            return null;
        }
        final Model maxModel = res.getResult().getModel();
        final SortedSet<Variable> maximumSatisfiableSet = maxModel.positiveVariables();
        growSolver.loadState(solverState);
        final SortedSet<Variable> minimumCorrectionSet = new TreeSet<>(variables);
        maximumSatisfiableSet.forEach(minimumCorrectionSet::remove);
        return LngResult.of(minimumCorrectionSet);
    }

    /**
     * The builder for an SMUS computation.
     */
    public static class Builder {
        private final FormulaFactory f;
        private final List<Formula> additionalConstraints = new ArrayList<>();
        private MaxSatConfig maxSatConfig = MaxSatConfig.CONFIG_OLL;

        private Builder(final FormulaFactory f) {
            this.f = f;
        }

        /**
         * Sets the list of additional constraints which must hold for the
         * computed SMUS. (Default: empty)
         * @param additionalConstraints the list of additional constraints.
         * @return the current builder
         */
        public Builder additionalConstraints(final Collection<Formula> additionalConstraints) {
            this.additionalConstraints.addAll(additionalConstraints);
            return this;
        }

        /**
         * Sets the MaxSat solver configuration for the SMUS computation.
         * (Default: OLL)
         * @param maxSatConfig the MaxSAT solver configuration
         * @return the current builder
         */
        public Builder maxSatConfig(final MaxSatConfig maxSatConfig) {
            this.maxSatConfig = maxSatConfig;
            return this;
        }

        /**
         * Builds the SMUS computation with the current builder's configuration.
         * @return the SMUS computation
         */
        public Smus build() {
            return new Smus(f, additionalConstraints, maxSatConfig);
        }
    }
}
