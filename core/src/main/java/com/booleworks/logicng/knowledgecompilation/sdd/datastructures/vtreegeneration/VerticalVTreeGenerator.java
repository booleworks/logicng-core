// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;

import java.util.ArrayList;
import java.util.Set;

/**
 * A generator for vertical vtrees.
 * <p>
 * A vertical vtree is a vtree that alternate between left linear and right
 * linear vtree where alternating the left children or the right children are a
 * leaves. For example: (a ((b (c d)) e)) is a right linear vtree.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VerticalVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    /**
     * Constructs a generator for a vertical vtree for a set of variables.
     * @param variables the variable of the vtree
     */
    public VerticalVTreeGenerator(final Set<Variable> variables) {
        this.variables = variables;
    }

    @Override
    public LngResult<VTree> generate(final VTreeRoot.Builder builder, final ComputationHandler handler) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }
        final ArrayList<Variable> varSet = new ArrayList<>(variables);
        return LngResult.of(generateRec(builder, varSet, 0, varSet.size() - 1, true));
    }

    private VTree generateRec(final VTreeRoot.Builder builder, final ArrayList<Variable> variables, final int first,
                              final int last, final boolean isLeft) {
        if (first == last) {
            return builder.vTreeLeaf(variables.get(first));
        }
        final VTree left;
        final VTree right;
        if (isLeft) {
            right = builder.vTreeLeaf(variables.get(last));
            left = generateRec(builder, variables, first, last - 1, false);
        } else {
            left = builder.vTreeLeaf(variables.get(first));
            right = generateRec(builder, variables, first + 1, last - 1, true);
        }
        return builder.vTreeInternal(left, right);
    }
}
