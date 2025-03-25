package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.Collection;

public class VTreeUtil {
    private VTreeUtil() {
    }

    public static VTree substituteNode(final VTree root, final VTree oldNode, final VTree newNode,
                                       final SddFactory sf) {
        if (root == oldNode) {
            return newNode;
        }
        if (root instanceof VTreeInternal) {
            return sf.vTreeInternal(
                    substituteNode(((VTreeInternal) root).getLeft(), oldNode, newNode, sf),
                    substituteNode((((VTreeInternal) root).getRight()), oldNode, newNode, sf)
            );
        } else {
            return root;
        }
    }

    public static VTree lcaFromVariables(final Collection<Variable> variables, final VTreeRoot root) {
        int posMax = Integer.MIN_VALUE;
        int posMin = Integer.MAX_VALUE;
        for (final Variable variable : variables) {
            final int pos = root.getPosition(root.getLeaf(variable));
            posMax = Math.max(posMax, pos);
            posMin = Math.min(posMin, pos);
        }
        return root.lcaOf(posMin, posMax);
    }
}
