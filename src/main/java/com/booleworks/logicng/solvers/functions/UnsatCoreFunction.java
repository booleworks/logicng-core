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
    public UNSATCore<Proposition> apply(final SATSolver solver) {
        if (!solver.config().proofGeneration()) {
            throw new IllegalStateException("Cannot generate an unsat core if proof generation is not turned on");
        }
        final DRUPTrim trimmer = new DRUPTrim();

        final Map<Formula, Proposition> clause2proposition = new HashMap<>();
        final LNGVector<LNGIntVector> clauses = new LNGVector<>(solver.underlyingSolver().pgOriginalClauses().size());
        for (final LNGCoreSolver.ProofInformation pi : solver.underlyingSolver().pgOriginalClauses()) {
            clauses.push(pi.clause());
            final Formula clause = getFormulaForVector(solver, pi.clause());
            Proposition proposition = pi.proposition();
            if (proposition == null) {
                proposition = new StandardProposition(clause);
            }
            clause2proposition.put(clause, proposition);
        }

        if (containsEmptyClause(clauses)) {
            final Proposition emptyClause = clause2proposition.get(solver.factory().falsum());
            return new UNSATCore<>(Collections.singletonList(emptyClause), true);
        }

        final DRUPTrim.DRUPResult result = trimmer.compute(clauses, solver.underlyingSolver().pgProof());
        if (result.trivialUnsat()) {
            return handleTrivialCase(solver);
        }
        final LinkedHashSet<Proposition> propositions = new LinkedHashSet<>();
        for (final LNGIntVector vector : result.unsatCore()) {
            propositions.add(clause2proposition.get(getFormulaForVector(solver, vector)));
        }
        return new UNSATCore<>(new ArrayList<>(propositions), false);
    }

    private UNSATCore<Proposition> handleTrivialCase(final SATSolver solver) {
        final LNGVector<LNGCoreSolver.ProofInformation> clauses = solver.underlyingSolver().pgOriginalClauses();
        for (int i = 0; i < clauses.size(); i++) {
            for (int j = i + 1; j < clauses.size(); j++) {
                if (clauses.get(i).clause().size() == 1 && clauses.get(j).clause().size() == 1 &&
                        clauses.get(i).clause().get(0) + clauses.get(j).clause().get(0) == 0) {
                    final LinkedHashSet<Proposition> propositions = new LinkedHashSet<>();
                    final Proposition pi = clauses.get(i).proposition();
                    final Proposition pj = clauses.get(j).proposition();
                    propositions.add(pi != null ? pi
                            : new StandardProposition(getFormulaForVector(solver, clauses.get(i).clause())));
                    propositions.add(pj != null ? pj
                            : new StandardProposition(getFormulaForVector(solver, clauses.get(j).clause())));
                    return new UNSATCore<>(new ArrayList<>(propositions), false);
                }
            }
        }
        throw new IllegalStateException("Should be a trivial unsat core, but did not found one.");
    }

    private boolean containsEmptyClause(final LNGVector<LNGIntVector> clauses) {
        for (final LNGIntVector clause : clauses) {
            if (clause.empty()) {
                return true;
            }
        }
        return false;
    }

    private Formula getFormulaForVector(final SATSolver solver, final LNGIntVector vector) {
        final List<Literal> literals = new ArrayList<>(vector.size());
        for (int i = 0; i < vector.size(); i++) {
            final int lit = vector.get(i);
            final String varName = solver.underlyingSolver().nameForIdx(Math.abs(lit) - 1);
            literals.add(solver.factory().literal(varName, lit > 0));
        }
        return solver.factory().or(literals);
    }
}
