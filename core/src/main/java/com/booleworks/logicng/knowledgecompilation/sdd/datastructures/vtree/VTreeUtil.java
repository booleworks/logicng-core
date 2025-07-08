package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

import java.util.Collection;
import java.util.Set;
import java.util.Stack;

public class VTreeUtil {
    private VTreeUtil() {
    }

    public static boolean isLeftFragment(final VTree vtree) {
        return !vtree.isLeaf() && !vtree.asInternal().getLeft().isLeaf();
    }

    public static boolean isRightFragment(final VTree vtree) {
        return !vtree.isLeaf() && !vtree.asInternal().getRight().isLeaf();
    }

    public static VTree substituteNode(final VTree root, final VTree oldNode, final VTree newNode, final Sdd sf) {
        if (root == oldNode) {
            return newNode;
        }
        if (root instanceof VTreeInternal) {
            return sf.vTreeInternal(
                    substituteNode(((VTreeInternal) root).getLeft(), oldNode, newNode, sf),
                    substituteNode((((VTreeInternal) root).getRight()), oldNode, newNode, sf));
        } else {
            return root;
        }
    }

    public static VTree lcaFromVariables(final Collection<Integer> variables, final Sdd sdd) {
        int posMax = Integer.MIN_VALUE;
        int posMin = Integer.MAX_VALUE;
        for (final int variable : variables) {
            final int pos = sdd.vTreeLeaf(variable).getPosition();
            posMax = Math.max(posMax, pos);
            posMin = Math.min(posMin, pos);
        }
        return sdd.getVTree().lcaOf(posMin, posMax);
    }

    public static void gapVars(final VTree vtree, final VTree subtree, final VTreeRoot root, final Set<Integer> filter,
                               final Set<Integer> result) {
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

    public static <C extends Collection<VTreeLeaf>> C leavesInOrder(final VTree vtree, final C result) {
        final Stack<VTree> stack = new Stack<>();
        stack.push(vtree);
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            if (current.isLeaf()) {
                result.add(current.asLeaf());
            } else {
                stack.add(current.asInternal().getRight());
                stack.add(current.asInternal().getLeft());
            }
        }
        return result;
    }

    public static void vars(final VTree vtree, final Set<Integer> filter, final Set<Integer> result) {
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
                                  final Set<Integer> filter) {
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

    public static int varCount(final VTree vtree, final Set<Integer> filter) {
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

    public static int varCount(final VTree vtree) {
        if (vtree.isLeaf()) {
            return 1;
        } else {
            return varCount(vtree.asInternal().getLeft()) + varCount(vtree.asInternal().getRight());
        }
    }
}
