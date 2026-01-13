//  SPDX-License-Identifier: Apache-2.0 and MIT
//  Copyright 2015-2023 Christoph Zengler
//  Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;

import java.util.Collection;

/**
 * An encoding result that writes the encoding directly to a MaxSat Solver.
 * @version 3.0.0
 * @since 3.0.0
 */
public class EncodingResultMaxSat implements EncodingResult {
    protected final MaxSatSolver solver;
    protected final MaxSat underlyingSolver;
    protected final FormulaFactory f;

    /**
     * Constructs a new encoding result that writes the result directly to a MaxSat Solver.
     * @param f      the factory
     * @param solver the solver instance
     */
    public EncodingResultMaxSat(final FormulaFactory f, final MaxSatSolver solver) {
        this.f = f;
        this.solver = solver;
        this.underlyingSolver = solver.getUnderlyingSolver();
    }

    /**
     * Adds a hard clause to the solver.
     * @param literals the literals of the clause
     */
    @Override
    public void addClause(final Literal... literals) {
        solver.resetResult();
        final LngIntVector clauseVec = new LngIntVector(literals.length);
        for (final Literal literal : literals) {
            addLiteral(clauseVec, literal);
        }
        underlyingSolver.addClause(clauseVec, -1);
    }

    /**
     * Adds a hard clause to the solver.
     * @param literals the literals of the clause
     */
    @Override
    public void addClause(final Collection<Literal> literals) {
        solver.resetResult();
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal l : literals) {
            addLiteral(clauseVec, l);
        }
        underlyingSolver.addClause(clauseVec, -1);
    }

    /**
     * Adds a hard clause to the solver.
     * @param literals the literals of the clause
     */
    @Override
    public void addClause(final LngVector<Literal> literals) {
        solver.resetResult();
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal l : literals) {
            addLiteral(clauseVec, l);
        }
        underlyingSolver.addClause(clauseVec, -1);
    }

    /**
     * Adds a soft clause to the solver.
     * @param weight   the weight of the clause
     * @param literals the clause
     */
    public void addSoftClause(final int weight, final Literal... literals) {
        solver.resetResult();
        final LngIntVector clauseVec = new LngIntVector(literals.length);
        for (final Literal literal : literals) {
            addLiteral(clauseVec, literal);
        }
        underlyingSolver.addClause(clauseVec, weight);
    }

    /**
     * Adds a soft clause to the solver.
     * @param weight   the weight of the clause
     * @param literals the clause
     */
    public void addSoftClause(final int weight, final Collection<Literal> literals) {
        solver.resetResult();
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal l : literals) {
            addLiteral(clauseVec, l);
        }
        underlyingSolver.addClause(clauseVec, weight);
    }

    /**
     * Adds a soft clause to the solver.
     * @param weight   the weight of the clause
     * @param literals the clause
     */
    public void addSoftClause(final int weight, final LngVector<Literal> literals) {
        solver.resetResult();
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal l : literals) {
            addLiteral(clauseVec, l);
        }
        underlyingSolver.addClause(clauseVec, weight);
    }

    private void addLiteral(final LngIntVector clauseVec, final Literal lit) {
        clauseVec.unsafePush(underlyingSolver.literal(lit));
    }

    @Override
    public Variable newVariable(final String auxType) {
        final int index = underlyingSolver.nVars();
        final String name = "@AUX_" + auxType + "_MAX_SAT_SOLVER_" + index;
        final Variable v = new EncodingAuxiliaryVariable(name);
        underlyingSolver.literal(v);
        return v;
    }

    @Override
    public FormulaFactory getFactory() {
        return f;
    }

    /**
     * Returns the solver.
     * @return the solver
     */
    public MaxSatSolver getSolver() {
        return solver;
    }
}
