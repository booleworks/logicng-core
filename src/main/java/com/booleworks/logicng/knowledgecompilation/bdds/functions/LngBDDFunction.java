// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.functions;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BDDConstant;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BDDInnerNode;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BDDNode;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates a LogicNG internal BDD data structure of a given BDD.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class LngBDDFunction extends BDDFunction<BDDNode> {

    public LngBDDFunction(final FormulaFactory f) {
        super(f);
    }

    @Override
    public BDDNode apply(final BDD bdd) {
        final BDDKernel kernel = bdd.underlyingKernel();
        final int index = bdd.index();
        final Map<Integer, int[]> kernelNodeMap = new BDDOperations(kernel).allNodes(index).stream()
                .collect(Collectors.toMap(node -> node[0], node -> node));
        return buildBDDNode(index, kernel, kernelNodeMap, new HashMap<>());
    }

    private BDDNode buildBDDNode(final int index, final BDDKernel kernel, final Map<Integer, int[]> kernelNodeMap,
                                 final Map<Integer, BDDNode> nodeMap) {
        BDDNode node = nodeMap.get(index);
        if (node != null) {
            return node;
        }
        if (index == BDDKernel.BDD_FALSE) {
            node = BDDConstant.getFalsumNode(kernel.factory());
        } else if (index == BDDKernel.BDD_TRUE) {
            node = BDDConstant.getVerumNode(kernel.factory());
        } else {
            final int[] kernelNode = kernelNodeMap.get(index);
            final Variable variable = kernel.getVariableForIndex(kernelNode[1]);
            final BDDNode lowNode = buildBDDNode(kernelNode[2], kernel, kernelNodeMap, nodeMap);
            final BDDNode highNode = buildBDDNode(kernelNode[3], kernel, kernelNodeMap, nodeMap);
            node = new BDDInnerNode(variable, lowNode, highNode);
        }
        nodeMap.put(index, node);
        return node;
    }
}
