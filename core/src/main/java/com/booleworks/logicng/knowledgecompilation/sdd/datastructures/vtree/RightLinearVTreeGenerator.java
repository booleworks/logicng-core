package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.ArrayList;
import java.util.Set;

public class RightLinearVTreeGenerator implements VTreeGenerator {
    private final static RightLinearVTreeGenerator INSTANCE = new RightLinearVTreeGenerator();

    public static RightLinearVTreeGenerator get() {
        return INSTANCE;
    }

    @Override
    public VTree generate(final SddFactory sf, final Set<Variable> variables) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        final ArrayList<Variable> varSet = new ArrayList<>(variables);
        int index = varSet.size() - 1;
        VTree right = null;
        while (index >= 0) {
            final VTreeLeaf left = sf.vTreeLeaf(varSet.get(index));
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
