package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.ArrayList;
import java.util.Set;

public class BalancedVTreeGenerator implements VTreeGenerator {
    private final static BalancedVTreeGenerator INSTANCE = new BalancedVTreeGenerator();

    public static BalancedVTreeGenerator get() {
        return INSTANCE;
    }

    @Override
    public VTree generate(final SddFactory sf, final Set<Variable> variables) {
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct VTree from a empty set of variables");
        }
        final ArrayList<Variable> varSet = new ArrayList<>(variables);
        return generateRec(sf, varSet, 0, varSet.size() - 1);
    }

    private VTree generateRec(final SddFactory sf, final ArrayList<Variable> variables, final int first,
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
