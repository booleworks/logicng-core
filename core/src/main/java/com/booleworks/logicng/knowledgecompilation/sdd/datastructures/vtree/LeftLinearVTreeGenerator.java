package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.ArrayList;
import java.util.Set;

public class LeftLinearVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    public LeftLinearVTreeGenerator(final Set<Variable> variables) {
        this.variables = variables;
    }

    @Override
    public LngResult<VTree> generate(final SddFactory sf, final ComputationHandler handler) {
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
