// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;

import java.util.Collection;
import java.util.SortedSet;

/**
 * An interface for split variable providers for model enumeration functions.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface SplitVariableProvider {

    /**
     * Generates a set of split variables for a given formula.
     * <p>
     * Such a set of split variables can then be used for the {@link ModelEnumerationFunction}.
     * @param solver    the solver
     * @param variables the variables from which the split variables should be chosen
     * @return the split variables
     */
    SortedSet<Variable> getSplitVars(final SATSolver solver, final Collection<Variable> variables);
}
