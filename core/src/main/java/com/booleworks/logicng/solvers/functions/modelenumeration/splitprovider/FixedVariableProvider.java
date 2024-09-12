// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.Collection;
import java.util.SortedSet;

/**
 * A split variable provider for which the variables are fixed.
 * @version 3.0.0
 * @since 3.0.0
 */
public class FixedVariableProvider implements SplitVariableProvider {

    private final SortedSet<Variable> splitVariables;

    /**
     * Creates a new split variables provider returning always the given split
     * variables.
     * @param splitVariables the variables to be returned in
     *                       {@link #splitVariables}
     */
    public FixedVariableProvider(final SortedSet<Variable> splitVariables) {
        this.splitVariables = splitVariables;
    }

    @Override
    public SortedSet<Variable> getSplitVars(final SATSolver solver, final Collection<Variable> variables) {
        return splitVariables;
    }

    @Override
    public String toString() {
        return "FixedVariableProvider{" +
                "splitVariables=" + splitVariables +
                '}';
    }
}
