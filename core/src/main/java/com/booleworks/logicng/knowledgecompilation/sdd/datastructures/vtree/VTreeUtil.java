package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.Collection;
import java.util.Set;

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

    public static void gapVars(final VTree vtree, final VTree subtree, final VTreeRoot root, final Set<Variable> filter,
                               final Set<Variable> result) {
        if (vtree == subtree) {
            return;
        }
        if (root.isSubtree(subtree, vtree.asInternal().getLeft())) {
            gapVars(vtree.asInternal().getLeft(), subtree, root, filter, result);
            vars(vtree.asInternal().getRight(), filter, result);
        } else {
            vars(vtree.asInternal().getLeft(), filter, result);
            gapVars(vtree.asInternal().getRight(), subtree, root, filter, result);
        }
    }

    public static void vars(final VTree vtree, final Set<Variable> filter, final Set<Variable> result) {
        if (vtree.isLeaf()) {
            if (filter == null || filter.contains(vtree.asLeaf().getVariable())) {
                result.add(vtree.asLeaf().getVariable());
            }
        } else {
            vars(vtree.asInternal().getLeft(), filter, result);
            vars(vtree.asInternal().getRight(), filter, result);
        }
    }

    public static int gapVarCount(final VTree vtree, final VTree subtree, final VTreeRoot root,
                                  final Set<Variable> filter) {
        if (vtree == subtree) {
            return 0;
        }
        if (root.isSubtree(subtree, vtree.asInternal().getLeft())) {
            return gapVarCount(vtree.asInternal().getLeft(), subtree, root, filter) + varCount(
                    vtree.asInternal().getRight(), filter);
        } else {
            return varCount(vtree.asInternal().getLeft(), filter) + gapVarCount(vtree.asInternal().getRight(), subtree,
                    root, filter);
        }
    }

    public static int varCount(final VTree vtree, final Set<Variable> filter) {
        if (vtree.isLeaf()) {
            if (filter.contains(vtree.asLeaf().getVariable())) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return varCount(vtree.asInternal().getLeft(), filter) + varCount(vtree.asInternal().getRight(), filter);
        }
    }
}
