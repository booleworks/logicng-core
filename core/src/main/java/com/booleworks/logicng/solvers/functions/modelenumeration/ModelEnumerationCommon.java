// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Common methods used by model enumeration implementations.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface ModelEnumerationCommon {

    /**
     * Generates a blocking clause from a given model and a set of relevant
     * variables.
     * @param modelFromSolver the current model for which the blocking clause
     *                        should be generated
     * @param relevantVars    the indices of the relevant variables. If
     *                        {@code null} all variables are relevant.
     * @return the blocking clause for the given model and relevant variables
     */
    static LNGIntVector generateBlockingClause(final LNGBooleanVector modelFromSolver,
                                               final LNGIntVector relevantVars) {
        final LNGIntVector blockingClause;
        if (relevantVars != null) {
            blockingClause = new LNGIntVector(relevantVars.size());
            for (int i = 0; i < relevantVars.size(); i++) {
                final int varIndex = relevantVars.get(i);
                if (varIndex != -1) {
                    final boolean varAssignment = modelFromSolver.get(varIndex);
                    blockingClause.push(varAssignment ? (varIndex * 2) ^ 1 : varIndex * 2);
                }
            }
        } else {
            blockingClause = new LNGIntVector(modelFromSolver.size());
            for (int i = 0; i < modelFromSolver.size(); i++) {
                final boolean varAssignment = modelFromSolver.get(i);
                blockingClause.push(varAssignment ? (i * 2) ^ 1 : i * 2);
            }
        }
        return blockingClause;
    }

    /**
     * Extracts the internal indices of a collection of variables from the
     * solver.
     * <p>
     * If {@code variables} is {@code null} and the solver does not include
     * auxiliary variables in the models, then this function returns the indices
     * of all variables on the solver. However, if {@code variables} is
     * {@code null} and the solver does include auxiliary variables, then it
     * returns {@code null}.
     * @param variables the variables for which the internal indices should be
     *                  extracted
     * @param solver    the solver from which the indices should be extracted
     * @return a list of the internal indices
     */
    static LNGIntVector relevantIndicesFromSolver(final Collection<Variable> variables, final SATSolver solver) {
        if (variables == null) {
            throw new IllegalArgumentException(
                    "Model enumeration must always be calles with a valid set of variables.");
        }
        final LNGIntVector relevantIndices;
        relevantIndices = new LNGIntVector(variables.size());
        for (final Variable var : variables) {
            relevantIndices.push(solver.getUnderlyingSolver().idxForName(var.getName()));
        }
        return relevantIndices;
    }

    /**
     * Extends a list of variables and their internal indices on the solver with
     * the internal indices of additional variables.
     * @param variables           the list of variables with already an internal
     *                            index extracted
     * @param additionalVariables the list of additional variable for which the
     *                            internal indices should be extracted
     * @param relevantIndices     the list of already obtained internal indices
     * @param solver              the solver from which the indices should be
     *                            extracted
     * @return {@code relevantIndices} + the newly obtained additional indices
     */
    static LNGIntVector relevantAllIndicesFromSolver(final Collection<Variable> variables,
                                                     final Collection<Variable> additionalVariables,
                                                     final LNGIntVector relevantIndices,
                                                     final SATSolver solver) {
        LNGIntVector relevantAllIndices = null;
        final SortedSet<Variable> uniqueAdditionalVariables =
                new TreeSet<>(additionalVariables == null ? Collections.emptyList() : additionalVariables);
        uniqueAdditionalVariables.removeAll(variables);
        if (relevantIndices != null) {
            if (uniqueAdditionalVariables.isEmpty()) {
                relevantAllIndices = relevantIndices;
            } else {
                relevantAllIndices = new LNGIntVector(relevantIndices.size() + uniqueAdditionalVariables.size());
                for (int i = 0; i < relevantIndices.size(); ++i) {
                    relevantAllIndices.push(relevantIndices.get(i));
                }
                for (final Variable var : uniqueAdditionalVariables) {
                    final int idx = solver.getUnderlyingSolver().idxForName(var.getName());
                    if (idx != -1) {
                        relevantAllIndices.push(idx);
                    }
                }
            }
        }
        return relevantAllIndices;
    }
}
