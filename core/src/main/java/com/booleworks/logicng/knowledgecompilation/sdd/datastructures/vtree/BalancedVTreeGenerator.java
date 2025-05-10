package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

import java.util.ArrayList;
import java.util.Set;

public class BalancedVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    public BalancedVTreeGenerator(final Set<Variable> variables) {
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
        return LngResult.of(generateRec(sf, varSet, 0, varSet.size() - 1));
    }

    private VTree generateRec(final Sdd sf, final ArrayList<Variable> variables, final int first,
                              final int last) {
        if (first == last) {
            return sf.vTreeLeaf(variables.get(first));
        } else {
            final int midIndex = first + ((last - first + 1) / 2) - 1;
            final VTree left = generateRec(sf, variables, first, midIndex);
            final VTree right = generateRec(sf, variables, midIndex + 1, last);
            return sf.vTreeInternal(left, right);
        }
    }
}
