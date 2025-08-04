package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

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
    public LngResult<VTree> generate(final Sdd sf, final ComputationHandler handler) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }
        final ArrayList<Variable> varSet = new ArrayList<>(variables);
        return LngResult.of(generateRec(sf, varSet, 0, varSet.size() - 1, true));
    }

    private VTree generateRec(final Sdd sf, final ArrayList<Variable> variables, final int first, final int last,
                              final boolean isLeft) {
        if (first == last) {
            return sf.vTreeLeaf(variables.get(first));
        }
        final VTree left;
        final VTree right;
        if (isLeft) {
            right = sf.vTreeLeaf(variables.get(last));
            left = generateRec(sf, variables, first, last - 1, false);
        } else {
            left = sf.vTreeLeaf(variables.get(first));
            right = generateRec(sf, variables, first + 1, last - 1, true);
        }
        return sf.vTreeInternal(left, right);
    }
}
