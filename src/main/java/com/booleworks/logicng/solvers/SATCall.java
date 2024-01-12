package com.booleworks.logicng.solvers;

import static com.booleworks.logicng.datastructures.Tristate.TRUE;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.functions.UnsatCoreFunction;
import com.booleworks.logicng.solvers.sat.MiniSat2Solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SATCall implements AutoCloseable {

    private final FormulaFactory f;
    private final MiniSat solverWrapper;
    private final MiniSat2Solver solver;
    private final SATHandler handler;
    private final List<? extends Literal> assumptions;
    private final List<? extends Literal> selectionOrder;
    private final List<? extends Formula> additionalFormulas;
    private SolverState initialState;
    private Tristate satState;

    public SATCall(final FormulaFactory f, final MiniSat solverWrapper, final SATHandler handler, final List<? extends Literal> assumptions, final List<? extends Literal> selectionOrder, final List<? extends Formula> additionalFormulas) {
        this.f = f;
        this.solverWrapper = solverWrapper;
        this.solver = ((MiniSat2Solver) solverWrapper.underlyingSolver());
        this.handler = handler;
        this.assumptions = assumptions;
        this.selectionOrder = selectionOrder;
        this.additionalFormulas = additionalFormulas;
        initAndSolve();
    }

    public static SATCallBuilder builder(final FormulaFactory f, final MiniSat solver) {
        return new SATCallBuilder(f, solver);
    }

    private void initAndSolve() {
        solver.startSatCall();
        solver.setHandler(handler);
        if (additionalFormulas != null) {
            initialState = solver.saveState();
        }
        if (selectionOrder != null) {
            solver.setSelectionOrder(selectionOrder);
        }
        if (assumptions != null) {
            solver.setAssumptions(generateClauseVector(assumptions));
        }
        this.satState = solver.internalSolve();
    }

    public Tristate getSatState() {
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
        if (satState == TRUE) {
            throw new IllegalStateException("An unsat core can only be generated if the formula is solved and is UNSAT");
        }
        if (satState == Tristate.UNDEF) {
            throw new IllegalStateException("Cannot generate an unsat core before the formula was solved.");
        }
        if (assumptions != null && !assumptions.isEmpty()) {
            // TODO: We could also add the assumptions here with save/load state and perform another solve call before computing the unsat core
            throw new IllegalStateException("Cannot compute an unsat core for a computation with assumptions.");
        }
        return solverWrapper.execute(UnsatCoreFunction.get());
    }

    /**
     * Creates an assignment from a Boolean vector of the solver.
     * @param vec             the vector of the solver
     * @param relevantIndices the solver's indices of the relevant variables for the model.
     * @return the assignment
     */
    public Assignment createAssignment(final LNGBooleanVector vec, final LNGIntVector relevantIndices) {
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
        if (assumptions != null) {
            solver.setAssumptions(new LNGIntVector());
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

    public static class SATCallBuilder {
        private final FormulaFactory f;
        private final MiniSat solver;
        private SATHandler handler;
        private List<? extends Literal> assumptions;
        private List<? extends Literal> selectionOrder;
        private List<? extends Formula> additionalFormulas;

        private SATCallBuilder(final FormulaFactory f, final MiniSat solver) {
            this.f = f;
            this.solver = solver;
        }

        public SATCall start() {
            return new SATCall(f, solver, handler, assumptions, selectionOrder, additionalFormulas);
        }

        public Tristate sat() {
            try (final SATCall call = start()) {
                return call.getSatState();
            }
        }

        public Assignment model(final Collection<Variable> variables) {
            try (final SATCall call = start()) {
                return call.model(variables);
            }
        }

        public Assignment model(final Variable... variables) {
            return model(Arrays.asList(variables));
        }

        public UNSATCore<Proposition> unsatCore() {
            try (final SATCall call = start()) {
                return call.unsatCore();
            }
        }

        public SATCallBuilder handler(final SATHandler handler) {
            this.handler = handler;
            return this;
        }

        public SATCallBuilder assumptions(final Collection<? extends Literal> assumptions) {
            this.assumptions = new ArrayList<>(assumptions);
            return this;
        }

        public SATCallBuilder assumptions(final Literal... assumptions) {
            this.assumptions = Arrays.asList(assumptions);
            return this;
        }

        public SATCallBuilder selectionOrder(final List<? extends Literal> selectionOrder) {
            this.selectionOrder = selectionOrder;
            return this;
        }

        public SATCallBuilder additionalFormulas(final List<? extends Formula> additionalFormulas) {
            this.additionalFormulas = additionalFormulas;
            return this;
        }
    }
}
