// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;

import java.util.Collection;
import java.util.SortedSet;

/**
 * A strategy for fine-tuning the model enumeration.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface ModelEnumerationStrategy {

    /**
     * Returns the maximum number of models to be enumerated on the given
     * recursion depth.
     * <p>
     * If this number of models is exceeded, the algorithm will compute new
     * split assignments and proceed to the next recursion step.
     * <p>
     * This number refers to actual enumerations on the solver, not to the
     * expanded number of models in the presence of don't care variables.
     * @param recursionDepth the current recursion depth starting with 0, the
     *                       first recursive call has depth 1
     * @return the maximum number of models for the enumeration step
     */
    int maxNumberOfModelsForEnumeration(int recursionDepth);

    /**
     * Returns the maximum number of models to be enumerated for split variables
     * on the given recursion depth.
     * <p>
     * This method is used to determine how many split assignments should at
     * most be computed and used for the next recursion step. If this limit is
     * exceeded, the algorithm will reduce the number of split variables using
     * {@link #reduceSplitVars} and then try again.
     * @param recursionDepth the current recursion depth starting with 0, the
     *                       first recursive call has depth 1
     * @return the maximum number of models for computation of split assignments
     */
    int maxNumberOfModelsForSplitAssignments(int recursionDepth);

    /**
     * Selects the split variables for the given recursion depth from the given
     * variables.
     * <p>
     * This method is called before the algorithm makes another recursive call
     * to determine the initial split variables for this call.
     * @param variables      the variables from which the split variables are
     *                       selected
     * @param solver         the solver (required for some variable selection
     *                       heuristics)
     * @param recursionDepth the recursion depth for the upcoming recursion
     *                       step, starting with 0, the first recursive call has
     *                       depth 1
     * @return the split variables which is a subset of the given variables
     */
    SortedSet<Variable> splitVarsForRecursionDepth(Collection<Variable> variables, SatSolver solver,
                                                   int recursionDepth);

    /**
     * Reduces the split variables for the given recursion depth in case of
     * {@link #maxNumberOfModelsForSplitAssignments} was exceeded.
     * @param variables      the variables to be reduced
     * @param recursionDepth the current recursion depth starting with 0, the
     *                       first recursive call has depth 1
     * @return the split variables which is a subset of the given variables
     */
    SortedSet<Variable> reduceSplitVars(Collection<Variable> variables, int recursionDepth);
}
