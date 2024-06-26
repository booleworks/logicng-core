package com.booleworks.logicng.solvers.sat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Builder for a {@link SATCall}. New instances should be created with
 * {@link SATSolver#satCall()}.
 * @see SATSolver#satCall()
 */
public class SATCallBuilder {
    private final SATSolver solver;
    private ComputationHandler handler;
    private final List<Proposition> additionalPropositions;
    private List<? extends Literal> selectionOrder;

    SATCallBuilder(final SATSolver solver) {
        this.solver = solver;
        handler = NopHandler.get();
        additionalPropositions = new ArrayList<>();
    }

    /**
     * Starts the actual solving process with the information passed to this
     * builder and returns a respective SAT call.
     * @return the SAT call
     */
    public SATCall solve() {
        return new SATCall(solver, handler, additionalPropositions, selectionOrder);
    }

    /**
     * Adds a handler to the SAT call.
     * @param handler the handler
     * @return this builder
     */
    public SATCallBuilder handler(final ComputationHandler handler) {
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
    public SATCallBuilder assumptions(final Collection<? extends Literal> assumptions) {
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
    public SATCallBuilder assumptions(final Literal... assumptions) {
        return addFormulas(assumptions);
    }

    /**
     * Adds additional formulas to the SAT call. The formulas are removed again
     * after the SAT call.
     * @param formulas the additional formulas
     * @return this builder
     */
    public SATCallBuilder addFormulas(final Collection<? extends Formula> formulas) {
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
    public SATCallBuilder addFormulas(final Formula... formulas) {
        return addFormulas(Arrays.asList(formulas));
    }

    /**
     * Adds additional propositions to the SAT call. The propositions are
     * removed again after the SAT call.
     * @param propositions the additional propositions
     * @return this builder
     */
    public SATCallBuilder addPropositions(final Collection<? extends Proposition> propositions) {
        additionalPropositions.addAll(propositions);
        return this;
    }

    /**
     * Adds additional propositions to the SAT call. The propositions are
     * removed again after the SAT call.
     * @param propositions the additional propositions
     * @return this builder
     */
    public SATCallBuilder addPropositions(final Proposition... propositions) {
        additionalPropositions.addAll(List.of(propositions));
        return this;
    }

    /**
     * Sets a custom selection order for this SAT call.
     * @param selectionOrder the selection order
     * @return this builder
     */
    public SATCallBuilder selectionOrder(final List<? extends Literal> selectionOrder) {
        this.selectionOrder = selectionOrder;
        return this;
    }

    // Utility methods, s.t. the user does not need to use the try-with resource

    /**
     * Directly computes the satisfiability result with the current state of the
     * builder. Returns {@link Tristate#TRUE} if the formula is satisfiable,
     * {@link Tristate#FALSE} if the formula is not satisfiable, and
     * {@link Tristate#UNDEF} if the SAT call was aborted by the
     * {@link SATHandler handler}.
     * @return the satisfiability result
     */
    public Tristate sat() {
        try (final SATCall call = solve()) {
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
     *         was unsatisfiable
     * @throws IllegalArgumentException if the given variables are {@code null}
     */
    public Assignment model(final Collection<Variable> variables) {
        try (final SATCall call = solve()) {
            return call.model(variables);
        }
    }

    /**
     * Directly computes an unsat core of the current problem.
     * <p>
     * {@link SATSolverConfig#proofGeneration() Proof generation} must be
     * enabled in order to use this method, otherwise an
     * {@link IllegalStateException} is thrown.
     * <p>
     * If the formula on the solver is satisfiable, {@code null} is returned.
     * <p>
     * @return the unsat core or {@code null} if the SAT call was satisfiable
     */
    public UNSATCore<Proposition> unsatCore() {
        try (final SATCall call = solve()) {
            return call.unsatCore();
        }
    }
}
