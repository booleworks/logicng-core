// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.explanations.drup.DrupTrim;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A solver function for computing the unsatisfiable core on the current solver.
 * @version 2.0.0
 * @since 2.0.0
 */
public final class UnsatCoreFunction implements SolverFunction<UnsatCore<Proposition>> {

    private final static UnsatCoreFunction INSTANCE = new UnsatCoreFunction();

    /**
     * Private empty constructor. Singleton class.
     */
    private UnsatCoreFunction() {
        // Intentionally left empty
    }

    /**
     * Returns the singleton of the function.
     * @return the function instance
     */
    public static UnsatCoreFunction get() {
        return INSTANCE;
    }

    @Override
    public LngResult<UnsatCore<Proposition>> apply(final SatSolver solver, final ComputationHandler handler) {
        if (!solver.getConfig().isProofGeneration()) {
            throw new IllegalStateException("Cannot generate an unsat core if proof generation is not turned on");
        }
        final DrupTrim trimmer = new DrupTrim();

        final Map<Formula, Proposition> clause2proposition = new HashMap<>();
        final LngVector<LngIntVector> clauses = new LngVector<>(solver.getUnderlyingSolver().pgOriginalClauses().size());
        for (final LngCoreSolver.ProofInformation pi : solver.getUnderlyingSolver().pgOriginalClauses()) {
            clauses.push(pi.getClause());
            final Formula clause = getFormulaForVector(solver, pi.getClause());
            Proposition proposition = pi.getProposition();
            if (proposition == null) {
                proposition = new StandardProposition(clause);
            }
            clause2proposition.put(clause, proposition);
        }

        if (containsEmptyClause(clauses)) {
            final Proposition emptyClause = clause2proposition.get(solver.getFactory().falsum());
            return LngResult.of(new UnsatCore<>(Collections.singletonList(emptyClause), true));
        }

        final DrupTrim.DrupResult result = trimmer.compute(clauses, solver.getUnderlyingSolver().pgProof());
        if (result.isTrivialUnsat()) {
            return LngResult.of(handleTrivialCase(solver));
        }
        final LinkedHashSet<Proposition> propositions = new LinkedHashSet<>();
        for (final LngIntVector vector : result.getUnsatCore()) {
            propositions.add(clause2proposition.get(getFormulaForVector(solver, vector)));
        }
        return LngResult.of(new UnsatCore<>(new ArrayList<>(propositions), false));
    }

    private UnsatCore<Proposition> handleTrivialCase(final SatSolver solver) {
        final LngVector<LngCoreSolver.ProofInformation> clauses = solver.getUnderlyingSolver().pgOriginalClauses();
        for (int i = 0; i < clauses.size(); i++) {
            for (int j = i + 1; j < clauses.size(); j++) {
                if (clauses.get(i).getClause().size() == 1 && clauses.get(j).getClause().size() == 1 &&
                        clauses.get(i).getClause().get(0) + clauses.get(j).getClause().get(0) == 0) {
                    final LinkedHashSet<Proposition> propositions = new LinkedHashSet<>();
                    final Proposition pi = clauses.get(i).getProposition();
                    final Proposition pj = clauses.get(j).getProposition();
                    propositions.add(pi != null ? pi
                            : new StandardProposition(getFormulaForVector(solver, clauses.get(i).getClause())));
                    propositions.add(pj != null ? pj
                            : new StandardProposition(getFormulaForVector(solver, clauses.get(j).getClause())));
                    return new UnsatCore<>(new ArrayList<>(propositions), false);
                }
            }
        }
        throw new IllegalStateException("Should be a trivial unsat core, but did not found one.");
    }

    private boolean containsEmptyClause(final LngVector<LngIntVector> clauses) {
        for (final LngIntVector clause : clauses) {
            if (clause.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Formula getFormulaForVector(final SatSolver solver, final LngIntVector vector) {
        final List<Literal> literals = new ArrayList<>(vector.size());
        for (int i = 0; i < vector.size(); i++) {
            final int lit = vector.get(i);
            final String varName = solver.getUnderlyingSolver().nameForIdx(Math.abs(lit) - 1);
            literals.add(solver.getFactory().literal(varName, lit > 0));
        }
        return solver.getFactory().or(literals);
    }
}
