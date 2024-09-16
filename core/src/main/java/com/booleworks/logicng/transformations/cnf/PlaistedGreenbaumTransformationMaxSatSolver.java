// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;

/**
 * A Plaisted-Greenbaum CNF conversion which is performed directly on the
 * internal MaxSAT solver, not on a formula factory.
 * @version 3.0.0
 * @since 1.6.0
 */
public final class PlaistedGreenbaumTransformationMaxSatSolver extends PlaistedGreenbaumCommon<Integer> {

    private final MaxSat solver;

    /**
     * Constructs a new transformation for a given MaxSAT solver.
     * @param f          the formula factory to generate new formulas
     * @param performNnf flag whether an NNF transformation should be
     *                   performed on the input formula
     * @param solver     the solver
     */
    public PlaistedGreenbaumTransformationMaxSatSolver(final FormulaFactory f, final boolean performNnf,
                                                       final MaxSat solver) {
        super(f, performNnf);
        this.solver = solver;
    }

    @Override
    void addCnf(final Formula cnf, final Integer weight) {
        switch (cnf.getType()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                solver.addClause(cnf, weight);
                break;
            case AND:
                for (final Formula clause : cnf) {
                    solver.addClause(clause, weight);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + cnf);
        }
    }

    @Override
    protected int newSolverVariable() {
        return solver.newVar() * 2;
    }

    @Override
    void addToSolver(final LngIntVector clause, final Integer addendum) {
        solver.addClause(clause, addendum);
    }

    @Override
    int getLitFromSolver(final Literal lit) {
        return solver.literal(lit);
    }
}
