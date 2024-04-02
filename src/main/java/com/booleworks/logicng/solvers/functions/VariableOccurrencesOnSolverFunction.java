// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.sat.LNGCoreSolver.var;
import static com.booleworks.logicng.util.CollectionHelper.nullSafe;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.datastructures.LNGClause;
import com.booleworks.logicng.solvers.datastructures.LNGVariable;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A solver function for counting variable occurrences on a SAT solver.
 * <p>
 * Note that these are usually not the same occurrences as in the original formula, since the
 * formula might have been converted to CNF and/or variables in clauses might have been subsumed.
 * @version 3.0.0
 * @since 3.0.0
 */
public class VariableOccurrencesOnSolverFunction implements SolverFunction<Map<Variable, Integer>> {

    private final Set<String> relevantVariables;

    /**
     * Creates a new function which counts all variables which are present on the solver.
     */
    public VariableOccurrencesOnSolverFunction() {
        this(null);
    }

    /**
     * Creates a new function which counts the occurrences of all the given relevant variables.
     * <p>
     * If a variable is not present on the solver, the respective count will be 0.
     * @param relevantVariables the relevant variables, in case of {@code null} all variables on the solver are counted
     */
    public VariableOccurrencesOnSolverFunction(final Set<Variable> relevantVariables) {
        this.relevantVariables = relevantVariables == null ? null : relevantVariables.stream().map(Variable::name).collect(Collectors.toSet());
    }

    @Override
    public Map<Variable, Integer> apply(final SATSolver solver) {
        final FormulaFactory f = solver.factory();
        final LNGCoreSolver underlyingSolver = solver.underlyingSolver();
        final Map<String, Integer> counts = initResultMap(underlyingSolver);
        for (final LNGClause clause : underlyingSolver.clauses()) {
            for (int i = 0; i < clause.size(); i++) {
                final String key = underlyingSolver.nameForIdx(var(clause.get(i)));
                counts.computeIfPresent(key, (u, old) -> old + 1);
            }
        }
        return counts.entrySet().stream().collect(Collectors.toMap(v -> f.variable(v.getKey()), Map.Entry::getValue));
    }

    private Map<String, Integer> initResultMap(final LNGCoreSolver underlyingSolver) {
        final Map<String, Integer> counts = new HashMap<>(); // start with Strings to prevent repeated variable lookups in FormulaFactory
        final LNGVector<LNGVariable> variables = underlyingSolver.variables();
        for (int i = 0; i < variables.size(); i++) {
            final LNGVariable var = variables.get(i);
            final String name = underlyingSolver.nameForIdx(i);
            if (relevantVariables == null || relevantVariables.contains(name)) {
                counts.put(name, var.level() == 0 ? 1 : 0);
            }
        }
        nullSafe(relevantVariables).forEach(v -> counts.putIfAbsent(v, 0));
        return counts;
    }
}
