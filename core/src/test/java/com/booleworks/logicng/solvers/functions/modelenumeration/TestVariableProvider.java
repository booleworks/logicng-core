// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import static java.util.Collections.emptySortedSet;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.SplitVariableProvider;

import java.util.Collection;
import java.util.SortedSet;

public class TestVariableProvider {
    public static class EmptyVariableProvider implements SplitVariableProvider {
        @Override
        public SortedSet<Variable> getSplitVars(final SatSolver solver, final Collection<Variable> variables) {
            return emptySortedSet();
        }
    }

    public static class NullVariableProvider implements SplitVariableProvider {
        @Override
        public SortedSet<Variable> getSplitVars(final SatSolver solver, final Collection<Variable> variables) {
            return null;
        }
    }
}

