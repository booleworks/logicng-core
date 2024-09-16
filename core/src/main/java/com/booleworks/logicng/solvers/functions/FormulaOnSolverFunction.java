// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.datastructures.Tristate.TRUE;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.CFalse;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngVariable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A solver function for getting the current formula on the solver.
 * <p>
 * Note that this formula is usually syntactically different to the formulas
 * which were actually added to the solver, since the formulas are added as CNF
 * and may be simplified or even removed depending on the state of the solver.
 * Furthermore, the solver might add learnt clauses or propagate literals.
 * <p>
 * If the formula on the solver is known to be unsatisfiable, this function will
 * add {@link CFalse falsum} to the returned set of formulas. However, as long
 * as {@link SatSolver#sat()} was not called on the current solver state, the
 * absence of {@link CFalse falsum} does not imply that the formula is
 * satisfiable.
 * <p>
 * Also note that formulas are not added to the solver as soon as the solver is
 * known be unsatisfiable.
 * @version 2.0.0
 * @since 2.0.0
 */
public final class FormulaOnSolverFunction implements SolverFunction<Set<Formula>> {

    private final static FormulaOnSolverFunction INSTANCE = new FormulaOnSolverFunction();

    /**
     * Private empty constructor. Singleton class.
     */
    private FormulaOnSolverFunction() {
        // Intentionally left empty
    }

    /**
     * Returns the singleton of the function.
     * @return the function instance
     */
    public static FormulaOnSolverFunction get() {
        return INSTANCE;
    }

    @Override
    public LngResult<Set<Formula>> apply(final SatSolver solver, final ComputationHandler handler) {
        final FormulaFactory f = solver.getFactory();
        final Set<Formula> formulas = new LinkedHashSet<>();
        for (final LngClause clause : solver.getUnderlyingSolver().getClauses()) {
            final List<Literal> lits = new ArrayList<>();
            for (int i = 0; i < clause.size(); i++) {
                final int litInt = clause.get(i);
                lits.add(f.literal(solver.getUnderlyingSolver().nameForIdx(litInt >> 1), (litInt & 1) != 1));
            }
            if (!clause.isAtMost()) {
                formulas.add(f.clause(lits));
            } else {
                final int rhs = clause.size() + 1 - clause.atMostWatchers();
                final List<Variable> vars = new ArrayList<>();
                for (final Literal lit : lits) {
                    vars.add(lit.variable());
                }
                formulas.add(f.cc(CType.LE, rhs, vars));
            }
        }
        final LngVector<LngVariable> variables = solver.getUnderlyingSolver().getVariables();
        for (int i = 0; i < variables.size(); i++) {
            final LngVariable var = variables.get(i);
            if (var.level() == 0) {
                formulas.add(f.literal(solver.getUnderlyingSolver().nameForIdx(i), var.assignment() == TRUE));
            }
        }
        if (!solver.getUnderlyingSolver().ok()) {
            formulas.add(f.falsum());
        }
        return LngResult.of(formulas);
    }
}
