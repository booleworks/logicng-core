// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SddNodesFunction implements SddFunction<SortedSet<SddNode>> {
    private final static SddNodesFunction INSTANCE = new SddNodesFunction();

    /**
     * Returns the singleton instance of this function.
     * @return the singleton instance
     */
    public static SddNodesFunction get() {
        return INSTANCE;
    }

    private SddNodesFunction() {
    }

    @Override
    public LngResult<SortedSet<SddNode>> execute(final SddNode node, final ComputationHandler handler) {
        final TreeSet<SddNode> nodes = new TreeSet<>();
        collectRec(node, nodes);
        return LngResult.of(nodes);
    }

    private void collectRec(final SddNode node, final Set<SddNode> dst) {
        if (dst.add(node)) {
            if (node.isDecomposition()) {
                for (final SddElement element : node.asDecomposition()) {
                    collectRec(element.getPrime(), dst);
                    collectRec(element.getSub(), dst);
                }
            }
        }
    }
}
