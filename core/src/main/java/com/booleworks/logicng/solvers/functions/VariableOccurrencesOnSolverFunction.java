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

    protected VariableOccurrencesOnSolverFunction(final Builder builder) {
        this.relevantVariables = builder.relevantVariables == null ? null
                                                                   : builder.relevantVariables.stream()
                                         .map(Variable::getName).collect(Collectors.toSet());
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

    /**
     * Returns the builder for this function.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for this function.
     */
    public static final class Builder {
        private Set<Variable> relevantVariables = null;

        private Builder() {
        }

        /**
         * Sets the set of relevant variable for the function. If a variable is
         * not present on the solver, the respective count will be 0.
         * <p>
         * If no set of relevant variables is set or {@code relevantVariables}
         * is {@code null}, the result will contain all variables on the solver.
         * @param relevantVariables the relevant variables, in case of {@code null}
         *                          all variables on the solver are counted
         */
        public Builder relevantVariables(final Set<Variable> relevantVariables) {
            this.relevantVariables = relevantVariables;
            return this;
        }

        /**
         * Builds the function with the current builder's configuration.
         * @return the function
         */
        public VariableOccurrencesOnSolverFunction build() {
            return new VariableOccurrencesOnSolverFunction(this);
        }
    }
}
