package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.Collection;

/**
 * An encoding result that writes the encoding directly to a solver.
 */
public final class EncodingResultSolver implements EncodingResult {
    private final Proposition proposition;
    private final LngCoreSolver solver;
    private final FormulaFactory f;

    /**
     * Constructs a new encoding result that writes the result directly to a solver.
     * @param f           the factory
     * @param solver      the solver instance
     * @param proposition the original proposition
     */
    public EncodingResultSolver(final FormulaFactory f, final LngCoreSolver solver, final Proposition proposition) {
        this.f = f;
        this.solver = solver;
        this.proposition = proposition;
    }

    @Override
    public void addClause(final Literal... literals) {
        final LngIntVector clauseVec = new LngIntVector(literals.length);
        for (final Literal literal : literals) {
            addLiteral(clauseVec, literal);
        }
        solver.addClause(clauseVec, proposition);
    }

    @Override
    public void addClause(final Collection<Literal> literals) {
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal l : literals) {
            addLiteral(clauseVec, l);
        }
        solver.addClause(clauseVec, proposition);
    }

    @Override
    public void addClause(final LngVector<Literal> literals) {
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal l : literals) {
            addLiteral(clauseVec, l);
        }
        solver.addClause(clauseVec, proposition);
    }

    private void addLiteral(final LngIntVector clauseVec, final Literal lit) {
        int index = solver.idxForName(lit.getName());
        if (index == -1) {
            index = solver.newVar(!solver.getConfig().getInitialPhase(), true);
            solver.addName(lit.getName(), index);
        }
        final int litNum;
        if (lit instanceof EncodingAuxiliaryVariable) {
            litNum = !((EncodingAuxiliaryVariable) lit).negated ? index * 2 : (index * 2) ^ 1;
        } else {
            litNum = lit.getPhase() ? index * 2 : (index * 2) ^ 1;
        }
        clauseVec.unsafePush(litNum);
    }

    @Override
    public Variable newVariable(final String auxType) {
        final int index = solver.newVar(!solver.getConfig().getInitialPhase(), true);
        final String name = "@AUX_" + auxType + "_SAT_SOLVER_" + index;
        solver.addName(name, index);
        return new EncodingAuxiliaryVariable(name, false);
    }

    @Override
    public FormulaFactory getFactory() {
        return f;
    }

    /**
     * Returns the original proposition.
     * @return the original proposition
     */
    public Proposition getProposition() {
        return proposition;
    }

    /**
     * Returns the solver.
     * @return the solver
     */
    public LngCoreSolver getSolver() {
        return solver;
    }
}
