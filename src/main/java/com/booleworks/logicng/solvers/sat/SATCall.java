package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.datastructures.Tristate.FALSE;
import static com.booleworks.logicng.datastructures.Tristate.TRUE;
import static com.booleworks.logicng.solvers.sat.LNGCoreSolver.generateClauseVector;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.UnsatCoreFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates the data used for a single SAT call to a {@link SATSolver}.
 * <p>
 * A SAT Solver is stateful and features like assumptions, selection order,
 * handler etc require manual housekeeping. This class aims to facilitate the
 * interaction with the SAT solver.
 * <p>
 * <b>There can never be more than one SAT call for the same SAT solver. You
 * must always {@link #close()} an existing SAT call before creating a new
 * one.</b>
 * <p>
 * A SAT call is generated by acquiring a {@link SATCallBuilder} from a solver
 * using {@link SATSolver#satCall()} and then (after potentially adding
 * assumptions or a handler) creating the SAT call via
 * {@link SATCallBuilder#solve()}.
 * <p>
 * This class basically provides three methods:
 * <ul>
 * <li>{@link #getSatResult()} returns whether the formula on the solver was
 * satisfiable or not</li>
 * <li>{@link #model} returns a model of the solver if it is satisfiable</li>
 * <li>{@link #unsatCore()} returns an UNSAT core if the formula in
 * unsatisfiable (and {@link SATSolverConfig#proofGeneration proof generation}
 * is enabled)</li>
 * </ul>
 */
public class SATCall implements AutoCloseable {

    private final SATSolver solverWrapper;
    private final LNGCoreSolver solver;
    private SolverState initialState;
    private int pgOriginalClausesLength = -1;
    private Tristate satState;

    SATCall(final SATSolver solverWrapper, final SATHandler handler,
            final List<? extends Proposition> additionalPropositions, final List<? extends Literal> selectionOrder) {
        this.solverWrapper = solverWrapper;
        solver = solverWrapper.underlyingSolver();
        initAndSolve(handler, additionalPropositions, selectionOrder);
    }

    public static SATCallBuilder builder(final SATSolver solver) {
        return new SATCallBuilder(solver);
    }

    private void initAndSolve(final SATHandler handler, final List<? extends Proposition> additionalPropositions, final List<? extends Literal> selectionOrder) {
        solver.assertNotInSatCall();
        if (solver.config.proofGeneration) {
            pgOriginalClausesLength = solver.pgOriginalClauses.size();
        }
        final Additionals additionals = splitPropsIntoLiteralsAndFormulas(additionalPropositions);
        if (!additionals.additionalLiterals.isEmpty()) {
            solver.assumptions = generateClauseVector(additionals.additionalLiterals, solver);
            solver.assumptionPropositions = new LNGVector<>(additionals.propositionsForLiterals);
        }
        if (!additionals.additionalFormulas.isEmpty()) {
            initialState = solver.saveState();
            additionals.additionalFormulas.forEach(solverWrapper::add);
        }
        solver.startSatCall();
        solver.setHandler(handler);
        if (selectionOrder != null) {
            solver.setSelectionOrder(selectionOrder);
        }
        satState = solver.internalSolve();
    }

    private Additionals splitPropsIntoLiteralsAndFormulas(final List<? extends Proposition> additionalPropositions) {
        final List<Literal> additionalLiterals = new ArrayList<>();
        final List<Proposition> propositionsForLiterals = new ArrayList<>();
        final List<Proposition> additionalFormulas = new ArrayList<>();
        for (final Proposition prop : additionalPropositions) {
            if (prop.formula().type() == FType.LITERAL) {
                additionalLiterals.add(((Literal) prop.formula()));
                propositionsForLiterals.add(prop);
            } else {
                additionalFormulas.add(prop);
            }
        }
        return new Additionals(additionalLiterals, propositionsForLiterals, additionalFormulas);
    }

    /**
     * Returns the satisfiability result of this SAT call, i.e.
     * {@link Tristate#TRUE} if the formula is satisfiable,
     * {@link Tristate#FALSE} if the formula is not satisfiable, and
     * {@link Tristate#UNDEF} if the SAT call was aborted by the
     * {@link SATHandler handler}.
     * @return the satisfiability result of this SAT call
     */
    public Tristate getSatResult() {
        return satState;
    }

    /**
     * Returns a model of the current formula on the solver wrt. a given set of
     * variables. The variables must not be {@code null}.
     * <p>
     * If the formula is UNSAT, {@code null} will be returned.
     * @param variables the set of variables
     * @return a model of the current formula or {@code null} if the SAT call
     *         was unsatisfiable
     * @throws IllegalArgumentException if the given variables are {@code null}
     */
    public Assignment model(final Collection<Variable> variables) {
        if (variables == null) {
            throw new IllegalArgumentException("The given variables must not be null.");
        }
        if (satState != TRUE) {
            return null;
        } else {
            final List<Literal> unknowns = new ArrayList<>();
            final LNGIntVector relevantIndices = new LNGIntVector(variables.size());
            for (final Variable var : variables) {
                final int element = solver.idxForName(var.name());
                if (element != -1) {
                    relevantIndices.push(element);
                } else {
                    unknowns.add(var.negate(solver.f));
                }
            }
            final List<Literal> finalModel = solver.convertInternalModel(solver.model(), relevantIndices);
            finalModel.addAll(unknowns);
            return new Assignment(finalModel);
        }
    }

    /**
     * Returns an unsat core of the current problem.
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
        if (!solver.config().proofGeneration()) {
            throw new IllegalStateException("Cannot generate an unsat core if proof generation is not turned on");
        }
        if (satState != FALSE) {
            return null;
        }
        return UnsatCoreFunction.get().apply(solverWrapper);
    }

    @Override
    public void close() {
        solver.assumptions = new LNGIntVector();
        solver.assumptionPropositions = new LNGVector<>();
        if (solver.config.proofGeneration) {
            solver.pgOriginalClauses.shrinkTo(pgOriginalClausesLength);
        }
        solver.setSelectionOrder(List.of());
        if (initialState != null) {
            solver.loadState(initialState);
        }
        solver.setHandler(null);
        solver.finishSatCall();
    }

    private static class Additionals {
        private final List<Literal> additionalLiterals;
        private final List<Proposition> propositionsForLiterals;
        private final List<Proposition> additionalFormulas;

        private Additionals(final List<Literal> additionalLiterals, final List<Proposition> propositionsForLiterals,
                            final List<Proposition> additionalFormulas) {
            this.additionalLiterals = additionalLiterals;
            this.propositionsForLiterals = propositionsForLiterals;
            this.additionalFormulas = additionalFormulas;
        }
    }
}
