// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.functions;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BddConstant;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BddInnerNode;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BddNode;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates a LogicNG internal BDD data structure of a given BDD.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class LngBddFunction extends BddFunction<BddNode> {

    public LngBddFunction(final FormulaFactory f) {
        super(f);
    }

    @Override
    public BddNode apply(final Bdd bdd) {
        final BddKernel kernel = bdd.getUnderlyingKernel();
        final int index = bdd.getIndex();
        final Map<Integer, int[]> kernelNodeMap = new BddOperations(kernel).allNodes(index).stream()
                .collect(Collectors.toMap(node -> node[0], node -> node));
        return buildBddNode(index, kernel, kernelNodeMap, new HashMap<>());
    }

    private BddNode buildBddNode(final int index, final BddKernel kernel, final Map<Integer, int[]> kernelNodeMap,
                                 final Map<Integer, BddNode> nodeMap) {
        BddNode node = nodeMap.get(index);
        if (node != null) {
            return node;
        }
        if (index == BddKernel.BDD_FALSE) {
            node = BddConstant.getFalsumNode(kernel.getFactory());
        } else if (index == BddKernel.BDD_TRUE) {
            node = BddConstant.getVerumNode(kernel.getFactory());
        } else {
            final int[] kernelNode = kernelNodeMap.get(index);
            final Variable variable = kernel.getVariableForIndex(kernelNode[1]);
            final BddNode lowNode = buildBddNode(kernelNode[2], kernel, kernelNodeMap, nodeMap);
            final BddNode highNode = buildBddNode(kernelNode[3], kernel, kernelNodeMap, nodeMap);
            node = new BddInnerNode(variable, lowNode, highNode);
        }
        nodeMap.put(index, node);
        return node;
    }
}
