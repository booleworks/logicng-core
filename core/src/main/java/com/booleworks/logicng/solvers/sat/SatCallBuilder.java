// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SatSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Builder for a {@link SatCall}. New instances should be created with
 * {@link SatSolver#satCall()}.
 * @see SatSolver#satCall()
 */
public class SatCallBuilder {
    private final SatSolver solver;
    private ComputationHandler handler;
    private final List<Proposition> additionalPropositions;
    private List<? extends Literal> selectionOrder;

    SatCallBuilder(final SatSolver solver) {
        this.solver = solver;
        handler = NopHandler.get();
        additionalPropositions = new ArrayList<>();
    }

    /**
     * Starts the actual solving process with the information passed to this
     * builder and returns a respective SAT call.
     * @return the SAT call
     */
    public SatCall solve() {
        return new SatCall(solver, handler, additionalPropositions, selectionOrder);
    }

    /**
     * Adds a handler to the SAT call.
     * @param handler the handler
     * @return this builder
     */
    public SatCallBuilder handler(final ComputationHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Adds assumptions (i.e. additional formulas) to the SAT call. The
     * assumptions are removed again after the SAT call.
     * @param assumptions the assumptions
     * @return this builder
     */
    // TODO could be removed (when we're sure about the API)
    @Deprecated
    public SatCallBuilder assumptions(final Collection<? extends Literal> assumptions) {
        return addFormulas(assumptions);
    }

    /**
     * Adds assumptions (i.e. additional formulas) to the SAT call. The
     * assumptions are removed again after the SAT call.
     * @param assumptions the assumptions
     * @return this builder
     */
    // TODO could be removed (when we're sure about the API)
    @Deprecated
    public SatCallBuilder assumptions(final Literal... assumptions) {
        return addFormulas(assumptions);
    }

    /**
     * Adds additional formulas to the SAT call. The formulas are removed again
     * after the SAT call.
     * @param formulas the additional formulas
     * @return this builder
     */
    public SatCallBuilder addFormulas(final Collection<? extends Formula> formulas) {
        for (final Formula formula : formulas) {
            additionalPropositions.add(new StandardProposition(formula));
        }
        return this;
    }

    /**
     * Adds additional formulas to the SAT call. The formulas are removed again
     * after the SAT call.
     * @param formulas the additional formulas
     * @return this builder
     */
    // TODO do we need the varargs variants?
    public SatCallBuilder addFormulas(final Formula... formulas) {
        return addFormulas(Arrays.asList(formulas));
    }

    /**
     * Adds additional propositions to the SAT call. The propositions are
     * removed again after the SAT call.
     * @param propositions the additional propositions
     * @return this builder
     */
    public SatCallBuilder addPropositions(final Collection<? extends Proposition> propositions) {
        additionalPropositions.addAll(propositions);
        return this;
    }

    /**
     * Adds additional propositions to the SAT call. The propositions are
     * removed again after the SAT call.
     * @param propositions the additional propositions
     * @return this builder
     */
    public SatCallBuilder addPropositions(final Proposition... propositions) {
        additionalPropositions.addAll(List.of(propositions));
        return this;
    }

    /**
     * Sets a custom selection order for this SAT call.
     * @param selectionOrder the selection order
     * @return this builder
     */
    public SatCallBuilder selectionOrder(final List<? extends Literal> selectionOrder) {
        this.selectionOrder = selectionOrder;
        return this;
    }

    // Utility methods, s.t. the user does not need to use the try-with resource

    /**
     * Directly computes the satisfiability result with the current state of the
     * builder.
     * @return the satisfiability result
     */
    public LngResult<Boolean> sat() {
        try (final SatCall call = solve()) {
            return call.getSatResult();
        }
    }

    /**
     * Directly computes a model of the current formula on the solver wrt. a
     * given set of variables. The variables must not be {@code null}.
     * <p>
     * If the formula is UNSAT, {@code null} will be returned.
     * @param variables the set of variables
     * @return a model of the current formula or {@code null} if the SAT call
     * was unsatisfiable
     * @throws IllegalArgumentException if the given variables are {@code null}
     */
    public Model model(final Collection<Variable> variables) {
        try (final SatCall call = solve()) {
            return call.model(variables);
        }
    }

    /**
     * Directly computes an unsat core of the current problem.
     * <p>
     * {@link SatSolverConfig#isProofGeneration() Proof generation} must be
     * enabled in order to use this method, otherwise an
     * {@link IllegalStateException} is thrown.
     * <p>
     * If the formula on the solver is satisfiable, {@code null} is returned.
     * <p>
     * @return the unsat core or {@code null} if the SAT call was satisfiable
     */
    public UnsatCore<Proposition> unsatCore() {
        try (final SatCall call = solve()) {
            return call.unsatCore();
        }
    }
}
