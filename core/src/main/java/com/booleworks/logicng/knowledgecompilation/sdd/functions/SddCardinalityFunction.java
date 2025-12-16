// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeInternal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * A function computing the cardinality of an SDD, i.e. the minimal/maximal
 * number of variables that must/can be {@code true} in a model.
 * <p>
 * This function can be reused for multiple SDD nodes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddCardinalityFunction implements SddFunction<Integer> {
    protected final Sdd sdd;
    protected final Set<Variable> variables;
    protected final boolean maximize;

    /**
     * Constructs a new cardinality function.
     * <p>
     * This function can be reused for multiple SDD nodes.
     * @param sdd       the SDD container
     * @param maximize  If {@code false} the minimal cardinality is computed, if
     *                  {@code false} the maximal cardinality is computed.
     * @param variables the relevant variables
     */
    public SddCardinalityFunction(final Sdd sdd, final boolean maximize, final Collection<Variable> variables) {
        this.sdd = sdd;
        this.variables = new HashSet<>(variables);
        this.maximize = maximize;
    }

    @Override
    public LngResult<Integer> execute(final SddNode node, final ComputationHandler handler) {
        final SortedSet<Integer> sddVariables = node.variables();
        final Set<Integer> variableIdxs = SddUtil.varsToIndicesOnlyKnown(sdd, variables, new HashSet<>());
        final int variablesNotInSdd = (int) variables
                .stream()
                .filter(v -> !sdd.knows(v) || !sddVariables.contains(sdd.variableToIndex(v)))
                .count();
        final int cardinality;
        if (node.isFalse()) {
            return LngResult.of(-1);
        } else if (node.isTrue()) {
            cardinality = 0;
        } else {
            cardinality = applyRec(sdd, node, variableIdxs, sddVariables, new HashMap<>());
        }
        return LngResult.of(maximize ? variablesNotInSdd + cardinality : cardinality);
    }

    protected int applyRec(final Sdd sdd, final SddNode node, final Set<Integer> relevantVariables,
                           final Set<Integer> sddVariables,
                           final HashMap<SddNode, Integer> cache) {
        assert !node.isFalse();
        if (node.isTrue()) {
            return 0;
        }
        if (node.isLiteral()) {
            if (relevantVariables.contains(node.asTerminal().getVTree().getVariable())) {
                if (node.asTerminal().getPhase()) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
        final Integer cached = cache.get(node);
        if (cached != null) {
            return cached;
        }
        final SddNodeDecomposition decomp = node.asDecomposition();
        final VTreeInternal vTree = node.getVTree().asInternal();
        int cardinality = -1;
        for (final SddElement element : decomp) {
            if (element.getSub().isFalse()) {
                continue;
            }
            final int prime = applyRec(sdd, element.getPrime(), relevantVariables, sddVariables, cache);
            final int sub = applyRec(sdd, element.getSub(), relevantVariables, sddVariables, cache);

            if (maximize) {
                final VTree left = vTree.getLeft();
                final VTree right = vTree.getRight();
                final int primeGap =
                        VTreeUtil.gapVarCount(left, element.getPrime().getVTree(), sdd.getVTree(), sddVariables);
                final int subGap;
                if (element.getSub().isTrue()) {
                    subGap = VTreeUtil.varCount(vTree.getRight(), sddVariables);
                } else {
                    subGap = VTreeUtil.gapVarCount(right, element.getSub().getVTree(), sdd.getVTree(), sddVariables);
                }
                final int c = prime + sub + primeGap + subGap;
                if (cardinality == -1 || c > cardinality) {
                    cardinality = c;
                }
            } else {
                final int c = prime + sub;
                if (cardinality == -1 || c < cardinality) {
                    cardinality = c;
                }
            }
        }
        cache.put(node, cardinality);
        return cardinality;
    }
}
