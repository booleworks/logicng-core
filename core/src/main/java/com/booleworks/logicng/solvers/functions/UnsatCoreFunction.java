// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.explanations.drup.DRUPTrim;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

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
public final class UnsatCoreFunction implements SolverFunction<UNSATCore<Proposition>> {

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
    public LNGResult<UNSATCore<Proposition>> apply(final SATSolver solver, final ComputationHandler handler) {
        if (!solver.getConfig().isProofGeneration()) {
            throw new IllegalStateException("Cannot generate an unsat core if proof generation is not turned on");
        }
        final DRUPTrim trimmer = new DRUPTrim();

        final Map<Formula, Proposition> clause2proposition = new HashMap<>();
        final LNGVector<LNGIntVector> clauses = new LNGVector<>(solver.getUnderlyingSolver().pgOriginalClauses().size());
        for (final LNGCoreSolver.ProofInformation pi : solver.getUnderlyingSolver().pgOriginalClauses()) {
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
            return LNGResult.of(new UNSATCore<>(Collections.singletonList(emptyClause), true));
        }

        final DRUPTrim.DRUPResult result = trimmer.compute(clauses, solver.getUnderlyingSolver().pgProof());
        if (result.isTrivialUnsat()) {
            return LNGResult.of(handleTrivialCase(solver));
        }
        final LinkedHashSet<Proposition> propositions = new LinkedHashSet<>();
        for (final LNGIntVector vector : result.getUnsatCore()) {
            propositions.add(clause2proposition.get(getFormulaForVector(solver, vector)));
        }
        return LNGResult.of(new UNSATCore<>(new ArrayList<>(propositions), false));
    }

    private UNSATCore<Proposition> handleTrivialCase(final SATSolver solver) {
        final LNGVector<LNGCoreSolver.ProofInformation> clauses = solver.getUnderlyingSolver().pgOriginalClauses();
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
                    return new UNSATCore<>(new ArrayList<>(propositions), false);
                }
            }
        }
        throw new IllegalStateException("Should be a trivial unsat core, but did not found one.");
    }

    private boolean containsEmptyClause(final LNGVector<LNGIntVector> clauses) {
        for (final LNGIntVector clause : clauses) {
            if (clause.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Formula getFormulaForVector(final SATSolver solver, final LNGIntVector vector) {
        final List<Literal> literals = new ArrayList<>(vector.size());
        for (int i = 0; i < vector.size(); i++) {
            final int lit = vector.get(i);
            final String varName = solver.getUnderlyingSolver().nameForIdx(Math.abs(lit) - 1);
            literals.add(solver.getFactory().literal(varName, lit > 0));
        }
        return solver.getFactory().or(literals);
    }
}
