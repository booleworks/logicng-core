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
 * Generator for a balanced vtree.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class BalancedVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    /**
     * Construct new vtree generator function for the given variables.
     * @param variables the variables for the generation
     */
    public BalancedVTreeGenerator(final Set<Variable> variables) {
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
        return LngResult.of(generateRec(builder, varSet, 0, varSet.size() - 1));
    }

    private VTree generateRec(final VTreeRoot.Builder builder, final ArrayList<Variable> variables, final int first,
                              final int last) {
        if (first == last) {
            return builder.vTreeLeaf(variables.get(first));
        } else {
            final int midIndex = first + ((last - first + 1) / 2) - 1;
            final VTree left = generateRec(builder, variables, first, midIndex);
            final VTree right = generateRec(builder, variables, midIndex + 1, last);
            return builder.vTreeInternal(left, right);
        }
    }
}
