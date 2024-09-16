// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static com.booleworks.logicng.solvers.sat.LNGCoreSolver.generateClauseVector;
import static com.booleworks.logicng.solvers.sat.LNGCoreSolver.solverLiteral;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.InternalAuxVarType;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

/**
 * A Plaisted-Greenbaum CNF conversion which is performed directly on the
 * internal SAT solver, not on a formula factory.
 * @version 2.0.0
 * @since 1.6.0
 */
public final class PlaistedGreenbaumTransformationSolver extends PlaistedGreenbaumCommon<Proposition> {

    private final LNGCoreSolver solver;

    /**
     * Constructs a new transformation for a given SAT solver.
     * @param f          the formula factory to generate new formulas
     * @param performNNF flag whether an NNF transformation should be
     *                   performed on the input formula
     * @param solver     the solver
     */
    public PlaistedGreenbaumTransformationSolver(final FormulaFactory f, final boolean performNNF,
                                                 final LNGCoreSolver solver) {
        super(f, performNNF);
        this.solver = solver;
    }

    @Override
    protected void addCNF(final Formula cnf, final Proposition proposition) {
        switch (cnf.getType()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                solver.addClause(generateClauseVector(cnf.literals(f), solver), proposition);
                break;
            case AND:
                for (final Formula clause : cnf) {
                    solver.addClause(generateClauseVector(clause.literals(f), solver), proposition);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + cnf);
        }
    }

    @Override
    protected int newSolverVariable() {
        final int index = solver.newVar(!solver.getConfig().getInitialPhase(), true);
        final String name = InternalAuxVarType.CNF.getPrefix() + "SAT_SOLVER_" + index;
        solver.addName(name, index);
        return index * 2;
    }

    @Override
    void addToSolver(final LNGIntVector clause, final Proposition addendum) {
        solver.addClause(clause, addendum);
    }

    @Override
    int getLitFromSolver(final Literal lit) {
        return solverLiteral(lit, solver);
    }
}
