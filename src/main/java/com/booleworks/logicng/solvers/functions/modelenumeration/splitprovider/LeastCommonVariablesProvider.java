// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.Collection;
import java.util.SortedSet;

/**
 * A split variable provider which provides split variables which occur particularly seldom in the formulas on the solver. The variables occurring in the
 * formulas are sorted by their occurrence. This provider returns those variables with the smallest occurrence.
 * @version 3.0.0
 * @since 3.0.0
 */
public class LeastCommonVariablesProvider extends SplitVariableProviderWithTakeRate {

    /**
     * Creates a split variable provider returning the least common variables with a take rate of {@code 0.5}.
     */
    public LeastCommonVariablesProvider() {
        super(0.5, 18);
    }

    /**
     * Creates a split variable provider returning the least common variables.
     * <p>
     * The take rate specifies the number of variables which should be returned in {@link #getSplitVars}.
     * So the result will contain {@code Math.min(maximumNumberOfVariables, (int) Math.ceil(variables.size() * takeRate))} variables.
     * @param takeRate                 the take rate, must be &gt; 0 and &lt;=1
     * @param maximumNumberOfVariables the maximum number of variables which should be selected
     */
    public LeastCommonVariablesProvider(final double takeRate, final int maximumNumberOfVariables) {
        super(takeRate, maximumNumberOfVariables);
    }

    @Override
    public SortedSet<Variable> getSplitVars(final SATSolver solver, final Collection<Variable> variables) {
        return chooseVariablesByOccurrences(solver, variables, false);
    }
}
