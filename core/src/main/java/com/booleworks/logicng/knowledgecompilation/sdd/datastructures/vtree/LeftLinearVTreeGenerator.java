package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.ArrayList;
import java.util.Set;

public class LeftLinearVTreeGenerator implements VTreeGenerator {
    @Override
    public VTree generate(final SddFactory sf, final Set<Variable> variables) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
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
        return left;
    }
}
