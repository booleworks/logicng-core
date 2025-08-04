package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

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
    public LngResult<VTree> generate(final Sdd sf, final ComputationHandler handler) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }
        return LngResult.of(generateRightLinear(sf, new ArrayList<>(variables), null));
    }

    static VTree generateRightLinear(final Sdd sf, final List<Variable> variables, final VTree stub) {
        int index = variables.size() - 1;
        VTree right = stub;
        while (index >= 0) {
            final VTreeLeaf left = sf.vTreeLeaf(variables.get(index));
            index -= 1;
            if (right == null) {
                right = left;
            } else {
                right = sf.vTreeInternal(left, right);
            }
        }
        return right;
    }
}
