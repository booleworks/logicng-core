// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A generator for right linear vtrees.
 * <p>
 * A right linear vtree is a vtree where every left child is a leaf. For
 * example: (a (b (c d))) is a right linear vtree.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class RightLinearVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    /**
     * Constructs a generator for a right linear vtree for a set of variables.
     * @param variables the variable of the vtree
     */
    public RightLinearVTreeGenerator(final Set<Variable> variables) {
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
        return LngResult.of(generateRightLinear(builder, new ArrayList<>(variables), null));
    }

    static VTree generateRightLinear(final VTreeRoot.Builder builder, final List<Variable> variables,
                                     final VTree stub) {
        int index = variables.size() - 1;
        VTree right = stub;
        while (index >= 0) {
            final VTreeLeaf left = builder.vTreeLeaf(variables.get(index));
            index -= 1;
            if (right == null) {
                right = left;
            } else {
                right = builder.vTreeInternal(left, right);
            }
        }
        return right;
    }
}
