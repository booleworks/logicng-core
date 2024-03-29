package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.datastructures.Tristate.FALSE;
import static com.booleworks.logicng.datastructures.Tristate.TRUE;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.UnsatCoreFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SATCall implements AutoCloseable {

    private final FormulaFactory f;
    private final MiniSat solverWrapper;
    private final MiniSat2Solver solver;
    private final SATHandler handler;
    private final List<? extends Proposition> additionalPropositions;
    private final List<? extends Literal> selectionOrder;
    private SolverState initialState;
    private int pgOriginalClausesLength = -1;
    private Tristate satState;

    public SATCall(final FormulaFactory f, final MiniSat solverWrapper, final SATHandler handler,
                   final List<? extends Proposition> additionalPropositions,
                   final List<? extends Literal> selectionOrder) {
        this.f = f;
        this.solverWrapper = solverWrapper;
        solver = ((MiniSat2Solver) solverWrapper.underlyingSolver());
        this.handler = handler;
        this.additionalPropositions = additionalPropositions;
        this.selectionOrder = selectionOrder;
        initAndSolve();
    }

    public static SATCallBuilder builder(final FormulaFactory f, final MiniSat solver) {
        return new SATCallBuilder(f, solver);
    }

    private void initAndSolve() {
        solver.startSatCall();
        solver.setHandler(handler);
        if (solver.config.proofGeneration) {
            pgOriginalClausesLength = solver.pgOriginalClauses.size();
        }
        final Additionals additionals = splitPropsIntoLiteralsAndFormulas(additionalPropositions);
        if (!additionals.additionalLiterals.isEmpty()) {
            solver.assumptions = generateClauseVector(additionals.additionalLiterals);
            solver.assumptionPropositions = new LNGVector<>(additionals.propositionsForLiterals);
        }
        if (!additionals.additionalFormulas.isEmpty()) {
            initialState = solver.saveState();
            additionals.additionalFormulas.forEach(solverWrapper::add);
        }
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

    public Tristate getSatResult() {
        return satState;
    }

    public Assignment model(final Collection<Variable> variables) {
        if (satState != TRUE) {
            return null;
        } else {
            final LNGIntVector relevantIndices = new LNGIntVector(variables.size());
            for (final Variable var : variables) {
                relevantIndices.push(solver.idxForName(var.name()));
            }
            return createAssignment(solver.model(), relevantIndices);
        }
    }

    public UNSATCore<Proposition> unsatCore() {
        if (!solver.getConfig().proofGeneration()) {
            throw new IllegalStateException("Cannot generate an unsat core if proof generation is not turned on");
        }
        if (satState != FALSE) {
            return null;
        }
        return solverWrapper.execute(UnsatCoreFunction.get());
    }

    /**
     * Creates an assignment from a Boolean vector of the solver.
     * @param vec             the vector of the solver
     * @param relevantIndices the solver's indices of the relevant variables for the model.
     * @return the assignment
     */
    private Assignment createAssignment(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
        return new Assignment(createLiterals(vec, relevantIndices));
    }

    private List<Literal> createLiterals(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
        final List<Literal> literals = new ArrayList<>(vec.size());
        for (int i = 0; i < relevantIndices.size(); i++) {
            final int index = relevantIndices.get(i);
            if (index != -1) {
                final String name = solver.nameForIdx(index);
                literals.add(f.literal(name, vec.get(index)));
            }
        }
        return literals;
    }

    @Override
    public void close() {
        solver.assumptions = new LNGIntVector();
        solver.assumptionPropositions = new LNGVector<>();
        if (solver.config.proofGeneration) {
            solver.pgOriginalClauses.shrinkTo(pgOriginalClausesLength);
        }
        if (selectionOrder != null) {
            solver.setSelectionOrder(List.of());
        }
        if (initialState != null) {
            solver.loadState(initialState);
        }
        solver.setHandler(null);
        solver.finishSatCall();
    }

    /**
     * Generates a clause vector of a collection of literals.
     * @param literals the literals
     * @return the clause vector
     */
    // TODO remove and replace call with minisat method
    protected LNGIntVector generateClauseVector(final Collection<? extends Literal> literals) {
        final LNGIntVector clauseVec = new LNGIntVector(literals.size());
        for (final Literal lit : literals) {
            final int index = getOrAddIndex(lit);
            final int litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
            clauseVec.push(litNum);
        }
        return clauseVec;
    }

    protected int getOrAddIndex(final Literal lit) {
        int index = solver.idxForName(lit.name());
        if (index == -1) {
            index = solver.newVar(!solver.getConfig().initialPhase(), true);
            solver.addName(lit.name(), index);
        }
        return index;
    }

    private static class Additionals {
        private final List<Literal> additionalLiterals;
        private final List<Proposition> propositionsForLiterals;
        private final List<Proposition> additionalFormulas;

        private Additionals(final List<Literal> additionalLiterals, final List<Proposition> propositionsForLiterals, final List<Proposition> additionalFormulas) {
            this.additionalLiterals = additionalLiterals;
            this.propositionsForLiterals = propositionsForLiterals;
            this.additionalFormulas = additionalFormulas;
        }
    }
}
