// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Collection of utility functions for working with CSP problems and encodings.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspUtil {
    protected CspUtil() {
    }

    /**
     * Returns the subset of {@code variables} containing all variables
     * represented in {@code booleanVariables}, i.e., at least one boolean
     * variable that belongs to the encoding of the variable is on the solver.
     * @param booleanVariables boolean variables on the solver
     * @param variables        input integer variables
     * @param context          the encoding context
     * @return the subset of integer variables on the solver
     */
    public static SortedSet<IntegerVariable> getVariablesOnSolver(final Set<Variable> booleanVariables,
                                                                  final Collection<IntegerVariable> variables,
                                                                  final CspEncodingContext context) {
        final TreeSet<IntegerVariable> result = new TreeSet<>();
        for (final IntegerVariable intVar : variables) {
            for (final Variable v : context.getEncodingVariables(List.of(intVar))) {
                if (booleanVariables.contains(v)) {
                    result.add(intVar);
                    break;
                }
            }
        }
        return result;
    }
}
