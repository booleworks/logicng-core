// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.VariableOccurrencesOnSolverFunction;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Super class for variable providers which always return a subset of the given
 * variables. The number of selected variables is defined by the
 * {@link #takeRate} which is the ration (between 0 and 1) of selected
 * variables.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class SplitVariableProviderWithTakeRate implements SplitVariableProvider {
    protected final double takeRate;
    protected final int maximumNumberOfVariables;

    /**
     * Creates a new split variable provider with the given take rate.
     * @param takeRate                 the take rate, must be &gt; 0 and &lt;=1
     * @param maximumNumberOfVariables the maximum number of variables which
     *                                 should be selected
     */
    protected SplitVariableProviderWithTakeRate(final double takeRate, final int maximumNumberOfVariables) {
        if (takeRate < 0 || takeRate > 1) {
            throw new IllegalArgumentException("Take rate must be a value between 0 and 1");
        }
        this.takeRate = takeRate;
        this.maximumNumberOfVariables = maximumNumberOfVariables;
    }

    /**
     * Returns a subset of the most or least common variables. The number of
     * returned variables is defined by the take rate (see
     * {@link #numberOfVariablesToChoose}).
     * @param solver     the solver used to count the variable occurrences
     * @param variables  the variables to choose from, in case of {@code null}
     *                   all variables from the solver are considered
     * @param mostCommon {@code true} is the most common variables should be
     *                   selected, {@code false} if the least common variables
     *                   should be selected
     * @return a subset of the most or least common variables
     */
    protected SortedSet<Variable> chooseVariablesByOccurrences(final SatSolver solver,
                                                               final Collection<Variable> variables,
                                                               final boolean mostCommon) {
        final Comparator<Map.Entry<Variable, Integer>> comparator =
                mostCommon ? Map.Entry.comparingByValue(Comparator.reverseOrder()) : Map.Entry.comparingByValue();
        final Set<Variable> vars = variables == null ? null : new HashSet<>(variables);
        final Map<Variable, Integer> variableOccurrences =
                solver.execute(new VariableOccurrencesOnSolverFunction(vars));
        return variableOccurrences.entrySet().stream()
                .sorted(comparator)
                .limit(numberOfVariablesToChoose(vars != null ? vars : variableOccurrences.keySet()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the number of variables which should be chosen. This depends on
     * the number of variables and the {@link #takeRate}.
     * @param variables the variables
     * @return the number of variables which should be chosen
     */
    protected int numberOfVariablesToChoose(final Collection<Variable> variables) {
        return Math.min(maximumNumberOfVariables, (int) Math.ceil(variables.size() * takeRate));
    }
}
