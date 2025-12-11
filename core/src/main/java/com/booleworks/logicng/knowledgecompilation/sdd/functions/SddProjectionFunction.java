// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddQuantification;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A function for computing the projection of an SDD.
 * <p>
 * The function projects SDD nodes to a set of variables by performing boolean
 * existential quantifier elimination for all variables of the SDD not contained
 * in the set of projection variables.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddProjectionFunction implements SddFunction<SddNode> {
    private final Set<Variable> variables;
    private final Sdd sdd;

    /**
     * Constructs a new projection function.
     * <p>
     * The function projects SDD nodes to a set of variables by performing
     * boolean existential quantifier elimination for all variables of the SDD
     * not contained in the set of projection variables.
     * @param variables the projection variables (will be preserved)
     * @param sdd       the SDD container
     */
    public SddProjectionFunction(final Set<Variable> variables, final Sdd sdd) {
        this.variables = variables;
        this.sdd = sdd;
    }

    @Override
    public LngResult<SddNode> execute(final SddNode node, final ComputationHandler handler) {
        final Set<Integer> variablesOnSdd = node.variables();
        final Set<Integer> variableIdxs = SddUtil.varsToIndicesOnlyKnown(variables, sdd, new HashSet<>());
        final Set<Integer> variablesToEliminate = variablesOnSdd.stream()
                .filter(v -> !variableIdxs.contains(v))
                .collect(Collectors.toSet());
        if (variablesToEliminate.isEmpty()) {
            return LngResult.of(node);
        } else {
            return SddQuantification.exists(variablesToEliminate, node, sdd, handler);
        }
    }
}
