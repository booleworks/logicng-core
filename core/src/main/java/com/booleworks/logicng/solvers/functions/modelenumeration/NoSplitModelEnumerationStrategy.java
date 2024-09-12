// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

/**
 * A strategy for the model enumeration where there are no splits.
 * @version 3.0.0
 * @since 3.0.0
 */
public class NoSplitModelEnumerationStrategy implements ModelEnumerationStrategy {

    private static final NoSplitModelEnumerationStrategy INSTANCE = new NoSplitModelEnumerationStrategy();

    private NoSplitModelEnumerationStrategy() {
        // Singleton pattern
    }

    /**
     * Return the no-split strategy
     * @return the strategy
     */
    public static NoSplitModelEnumerationStrategy get() {
        return INSTANCE;
    }

    @Override
    public int maxNumberOfModelsForEnumeration(final int recursionDepth) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int maxNumberOfModelsForSplitAssignments(final int recursionDepth) {
        return Integer.MAX_VALUE;
    }

    @Override
    public SortedSet<Variable> splitVarsForRecursionDepth(final Collection<Variable> variables, final SATSolver solver,
                                                          final int recursionDepth) {
        return Collections.emptySortedSet();
    }

    @Override
    public SortedSet<Variable> reduceSplitVars(final Collection<Variable> variables, final int recursionDepth) {
        return Collections.emptySortedSet();
    }
}
