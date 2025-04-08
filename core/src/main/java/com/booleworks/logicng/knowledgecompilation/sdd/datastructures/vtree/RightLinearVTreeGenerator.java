package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RightLinearVTreeGenerator implements VTreeGenerator {
    private final Set<Variable> variables;

    public RightLinearVTreeGenerator(Set<Variable> variables) {
        this.variables = variables;
    }

    @Override
    public LngResult<VTree> generate(final SddFactory sf, final ComputationHandler handler) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        return LngResult.of(generateRightLinear(sf, new ArrayList<>(variables), null));
    }

    public static VTree generateRightLinear(final SddFactory sf, final List<Variable> variables, final VTree stub) {
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
