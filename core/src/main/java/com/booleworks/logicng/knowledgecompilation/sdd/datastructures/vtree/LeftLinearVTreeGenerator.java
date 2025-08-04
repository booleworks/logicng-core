package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

import java.util.ArrayList;
import java.util.Set;

/**
 * A generator for left linear vtrees.
 * <p>
 * A left linear vtree is a vtree where every right child is a leaf. For
 * example: (((a b) c) d) is a left linear vtree.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class LeftLinearVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    /**
     * Constructs a generator for a left linear vtree for a set of variables.
     * @param variables the variable of the vtree
     */
    public LeftLinearVTreeGenerator(final Set<Variable> variables) {
        this.variables = variables;
    }

    @Override
    public LngResult<VTree> generate(final Sdd sf, final ComputationHandler handler) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }
        final ArrayList<Variable> varSet = new ArrayList<>(variables);
        int index = 0;
        VTree left = null;
        while (index < varSet.size()) {
            final VTreeLeaf right = sf.vTreeLeaf(varSet.get(index));
            index += 1;
            if (left == null) {
                left = right;
            } else {
                left = sf.vTreeInternal(left, right);
            }
        }
        return LngResult.of(left);
    }
}
