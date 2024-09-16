// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.sat.LngCoreSolver.var;
import static com.booleworks.logicng.util.CollectionHelper.nullSafe;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngVariable;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A solver function for counting variable occurrences on a SAT solver.
 * <p>
 * Note that these are usually not the same occurrences as in the original
 * formula, since the formula might have been converted to CNF and/or variables
 * in clauses might have been subsumed.
 * @version 3.0.0
 * @since 3.0.0
 */
public class VariableOccurrencesOnSolverFunction implements SolverFunction<Map<Variable, Integer>> {

    private final Set<String> relevantVariables;

    /**
     * Creates a new function which counts all variables which are present on
     * the solver.
     */
    public VariableOccurrencesOnSolverFunction() {
        this(null);
    }

    /**
     * Creates a new function which counts the occurrences of all the given
     * relevant variables.
     * <p>
     * If a variable is not present on the solver, the respective count will be
     * 0.
     * @param relevantVariables the relevant variables, in case of {@code null}
     *                          all variables on the solver are counted
     */
    public VariableOccurrencesOnSolverFunction(final Set<Variable> relevantVariables) {
        this.relevantVariables = relevantVariables == null ? null
                : relevantVariables.stream().map(Variable::getName).collect(Collectors.toSet());
    }

    @Override
    public LngResult<Map<Variable, Integer>> apply(final SatSolver solver, final ComputationHandler handler) {
        final FormulaFactory f = solver.getFactory();
        final LngCoreSolver underlyingSolver = solver.getUnderlyingSolver();
        final Map<String, Integer> counts = initResultMap(underlyingSolver);
        for (final LngClause clause : underlyingSolver.getClauses()) {
            for (int i = 0; i < clause.size(); i++) {
                final String key = underlyingSolver.nameForIdx(var(clause.get(i)));
                counts.computeIfPresent(key, (u, old) -> old + 1);
            }
        }
        return LngResult.of(counts.entrySet().stream()
                .collect(Collectors.toMap(v -> f.variable(v.getKey()), Map.Entry::getValue)));
    }

    private Map<String, Integer> initResultMap(final LngCoreSolver underlyingSolver) {
        // start with Strings to prevent repeated variable lookups in
        // FormulaFactory
        final Map<String, Integer> counts = new HashMap<>();
        final LngVector<LngVariable> variables = underlyingSolver.getVariables();
        for (int i = 0; i < variables.size(); i++) {
            final LngVariable var = variables.get(i);
            final String name = underlyingSolver.nameForIdx(i);
            if (relevantVariables == null || relevantVariables.contains(name)) {
                counts.put(name, var.level() == 0 ? 1 : 0);
            }
        }
        nullSafe(relevantVariables).forEach(v -> counts.putIfAbsent(v, 0));
        return counts;
    }
}
