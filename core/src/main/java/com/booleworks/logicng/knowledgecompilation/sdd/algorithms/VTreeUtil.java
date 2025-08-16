package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.Stack;

/**
 * A collection of utility functions for VTrees.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VTreeUtil {
    private VTreeUtil() {
    }

    /**
     * Checks whether a VTree is a left fragment, i.e., an internal node whose
     * left child is also an internal node.
     * @param vtree the VTree
     * @return whether the VTree is a left fragment
     */
    public static boolean isLeftFragment(final VTree vtree) {
        return vtree != null && !vtree.isLeaf() && !vtree.asInternal().getLeft().isLeaf();
    }

    /**
     * Checks whether a VTree is a right fragment, i.e., an internal node whose
     * right child is also an internal node.
     * @param vtree the VTree
     * @return whether the VTree is a right fragment
     */
    public static boolean isRightFragment(final VTree vtree) {
        return vtree != null && !vtree.isLeaf() && !vtree.asInternal().getRight().isLeaf();
    }

    /**
     * Constructs a new VTree where {@code oldNode} is replaced with
     * {@code newNode} in {@code root}.
     * @param root    the whole VTree
     * @param oldNode the subtree supposed to replace
     * @param newNode the substitute
     * @param sdd     the SDD container
     * @return A copy of {@code root} where {@code oldNode} is replaced with {@code newNode}.
     */
    public static VTree substituteNode(final VTree root, final VTree oldNode, final VTree newNode, final Sdd sdd) {
        if (root == oldNode) {
            return newNode;
        }
        if (root instanceof VTreeInternal) {
            return sdd.vTreeInternal(
                    substituteNode(((VTreeInternal) root).getLeft(), oldNode, newNode, sdd),
                    substituteNode((((VTreeInternal) root).getRight()), oldNode, newNode, sdd));
        } else {
            return root;
        }
    }

    /**
     * Computes the lowest common ancestor of a set of variables in the
     * currently active VTree of the SDD.
     * <ul>
     * <li><i>Preconditions:</i> The SDD must have a VTree defined and all
     * variables in {@code variables} must be stored in the currently active
     * VTree, otherwise a call results in undefined behaviour.</li>
     * </ul>
     * @param variables the variables
     * @param sdd       the SDD container
     * @return the lowest common ancestor of {@code variables}
     */
    public static VTree lcaFromVariables(final Collection<Integer> variables, final Sdd sdd) {
        assert sdd.isVTreeDefined();
        int posMax = Integer.MIN_VALUE;
        int posMin = Integer.MAX_VALUE;
        for (final int variable : variables) {
            final int pos = sdd.vTreeLeaf(variable).getPosition();
            posMax = Math.max(posMax, pos);
            posMin = Math.min(posMin, pos);
        }
        return sdd.getVTree().lcaOf(posMin, posMax);
    }

    /**
     * Traverses the VTree in order and adds the leaves to {@code result}.
     * @param vtree  the VTree
     * @param result the collection to which the result is written
     * @param <C>    the type of the collection for the result
     * @return {@code result} with all leaves of the VTree in order.
     */
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

    /**
     * Traverses the VTree in order and adds all nodes (inner and leaves) to
     * {@code result}.
     * @param vtree  the VTree
     * @param result the collection to which the result is written
     * @param <C>    the type of the collection for the result
     * @return {@code result} with all nodes of the VTree in order.
     */
    public static <C extends Collection<VTree>> C nodesInOrder(final VTree vtree, final C result) {
        final Stack<VTree> stack = new Stack<>();
        stack.push(vtree);
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            result.add(current);
            if (!current.isLeaf()) {
                stack.add(current.asInternal().getRight());
                stack.add(current.asInternal().getLeft());
            }
        }
        return result;
    }

    /**
     * Traverses the VTree in order and adds all inner nodes to {@code result}.
     * @param vtree  the VTree
     * @param result the collection to which the result is written
     * @param <C>    the type of the collection for the result
     * @return {@code result} with all nodes of the VTree in order.
     */
    public static <C extends Collection<VTreeInternal>> C innerNodesInOrder(final VTree vtree, final C result) {
        final Stack<VTree> stack = new Stack<>();
        stack.push(vtree);
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            if (!current.isLeaf()) {
                result.add(current.asInternal());
                stack.add(current.asInternal().getRight());
                stack.add(current.asInternal().getLeft());
            }
        }
        return result;
    }

    /**
     * Traverses the VTree in order and adds the variables to {@code result}.
     * @param vtree  the VTree
     * @param result the collection to which the result is written
     * @param <C>    the type of the collection for the result
     * @return {@code result} with all variables of the VTree in order.
     */
    public static <C extends Collection<Integer>> C vars(final VTree vtree, final C result) {
        return vars(vtree, null, result);
    }

    /**
     * Traverses the VTree in order and adds all variables that also occur in
     * {@code filter} to {@code result}.
     * @param vtree  the VTree
     * @param filter the filter for the result
     * @param result the collection to which the result is written
     * @param <C>    the type of the collection for the result
     * @return {@code result} with all variables of the VTree that occur in {@code filter}.
     */
    public static <C extends Collection<Integer>> C vars(final VTree vtree, final Set<Integer> filter, final C result) {
        if (vtree == null) {
            return result;
        }
        if (vtree.isLeaf()) {
            if (filter == null || filter.contains(vtree.asLeaf().getVariable())) {
                result.add(vtree.asLeaf().getVariable());
            }
        } else {
            vars(vtree.asInternal().getLeft(), filter, result);
            vars(vtree.asInternal().getRight(), filter, result);
        }
        return result;
    }

    /**
     * Traverses the VTree in order and adds all variables that also occur in
     * {@code filter} to {@code result}.
     * @param vtree  the VTree
     * @param filter the filter for the result as variable bitmap
     * @param result the collection to which the result is written
     * @param <C>    the type of the collection for the result
     * @return {@code result} with all variables of the VTree that occur in {@code filter}.
     */
    public static <C extends Collection<Integer>> C varsMasked(final VTree vtree, final BitSet filter, final C result) {
        if (vtree == null) {
            return result;
        }
        if (vtree.isLeaf()) {
            if (filter == null || filter.get(vtree.asLeaf().getVariable())) {
                result.add(vtree.asLeaf().getVariable());
            }
        } else {
            varsMasked(vtree.asInternal().getLeft(), filter, result);
            varsMasked(vtree.asInternal().getRight(), filter, result);
        }
        return result;
    }

    /**
     * Counts the number of variables in {@code vtree}.
     * @param vtree the VTree
     * @return count of variables that are in {@code vtree}
     */
    public static int varCount(final VTree vtree) {
        if (vtree.isLeaf()) {
            return 1;
        } else {
            return varCount(vtree.asInternal().getLeft()) + varCount(vtree.asInternal().getRight());
        }
    }

    /**
     * Counts the number of variables in {@code vtree} that also occur in
     * {@code filter}.
     * @param vtree  the VTree
     * @param filter a filter of variables
     * @return count of variables that are in {@code vtree} and {@code filter}
     */
    public static int varCount(final VTree vtree, final Set<Integer> filter) {
        if (vtree.isLeaf()) {
            if (filter == null || filter.contains(vtree.asLeaf().getVariable())) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return varCount(vtree.asInternal().getLeft(), filter) + varCount(vtree.asInternal().getRight(), filter);
        }
    }

    /**
     * Computes the variables that are contained in {@code vtree} but not in
     * {@code subtree}.
     * <p>
     * The function stores the internal indices of variables in {@code result},
     * which it also returns.
     * <ul>
     * <li><i>Preconditions:</i> {@code vtree} and {@code subtree} must be part
     * of {@code root}, and {@code subtree} must be a subtree of or equal to
     * {@code vtree}.</li>
     * </ul>
     * @param vtree   the VTree
     * @param subtree a subtree
     * @param root    the root of both VTrees.
     * @param result  the collection to which the result is written
     * @param <C>     the type of the collection for the result
     * @return variables that are occur in {@code vtree} but not in
     * {@code subtree}
     */
    public static <C extends Collection<Integer>> C gapVars(final VTree vtree, final VTree subtree,
                                                            final VTreeRoot root, final C result) {
        return gapVars(vtree, subtree, root, null, result);
    }

    /**
     * Computes the variables that are contained in {@code vtree} and
     * {@code filter} but not in {@code subtree}.
     * <p>
     * The function stores the internal indices of variables in {@code result},
     * which it also returns.
     * <ul>
     * <li><i>Preconditions:</i> {@code vtree} and {@code subtree} must be part
     * of {@code root}, and {@code subtree} must be a subtree of or equal to
     * {@code vtree}.</li>
     * </ul>
     * @param vtree   the VTree
     * @param subtree a subtree
     * @param root    the root of both VTrees.
     * @param filter  a filter for the result
     * @param result  the collection to which the result is written
     * @param <C>     the type of the collection for the result
     * @return variables that are occur in {@code vtree} and {@code filter} but
     * not in {@code subtree}
     */
    public static <C extends Collection<Integer>> C gapVars(final VTree vtree, final VTree subtree,
                                                            final VTreeRoot root, final Set<Integer> filter,
                                                            final C result) {
        if (vtree == subtree) {
            return result;
        }
        if (subtree == null) {
            return vars(vtree, filter, result);
        }
        if (root.isSubtree(subtree, vtree.asInternal().getLeft())) {
            gapVars(vtree.asInternal().getLeft(), subtree, root, filter, result);
            vars(vtree.asInternal().getRight(), filter, result);
        } else {
            vars(vtree.asInternal().getLeft(), filter, result);
            gapVars(vtree.asInternal().getRight(), subtree, root, filter, result);
        }
        return result;
    }

    /**
     * Computes the variables that are contained in {@code vtree} and
     * {@code filter} but not in {@code subtree}.
     * <p>
     * The function stores the internal indices of variables in {@code result},
     * which it also returns.
     * <ul>
     * <li><i>Preconditions:</i> {@code vtree} and {@code subtree} must be part
     * of {@code root}, and {@code subtree} must be a subtree of or equal to
     * {@code vtree}.</li>
     * </ul>
     * @param vtree   the VTree
     * @param subtree a subtree
     * @param root    the root of both VTrees.
     * @param filter  a filter for the result as variable bitmap
     * @param result  the collection to which the result is written
     * @param <C>     the type of the collection for the result
     * @return variables that are occur in {@code vtree} and {@code filter} but
     * not in {@code subtree}
     */
    public static <C extends Collection<Integer>> C gapVarsMasked(final VTree vtree, final VTree subtree,
                                                                  final VTreeRoot root, final BitSet filter,
                                                                  final C result) {
        if (vtree == subtree) {
            return result;
        }
        if (subtree == null) {
            return varsMasked(vtree, filter, result);
        }
        if (root.isSubtree(subtree, vtree.asInternal().getLeft())) {
            gapVarsMasked(vtree.asInternal().getLeft(), subtree, root, filter, result);
            varsMasked(vtree.asInternal().getRight(), filter, result);
        } else {
            varsMasked(vtree.asInternal().getLeft(), filter, result);
            gapVarsMasked(vtree.asInternal().getRight(), subtree, root, filter, result);
        }
        return result;
    }

    /**
     * Counts the variables that are contained in {@code vtree} but not in
     * {@code subtree}.
     * <ul>
     * <li><i>Preconditions:</i> {@code vtree} and {@code subtree} must be part
     * of {@code root}, and {@code subtree} must be a subtree of or equal to
     * {@code vtree}.</li>
     * </ul>
     * @param vtree   the VTree
     * @param subtree a subtree
     * @param root    the root of both VTrees.
     * @return count of variables that occurring in {@code vtree} but not in
     * {@code subtree}
     */
    public static int gapVarCount(final VTree vtree, final VTree subtree, final VTreeRoot root) {
        return gapVarCount(vtree, subtree, root, null);
    }

    /**
     * Counts the variables that are contained in {@code vtree} and
     * {@code filter} but not in {@code subtree}.
     * <ul>
     * <li><i>Preconditions:</i> {@code vtree} and {@code subtree} must be part
     * of {@code root}, and {@code subtree} must be a subtree of or equal to
     * {@code vtree}.</li>
     * </ul>
     * @param vtree   the VTree
     * @param subtree a subtree
     * @param root    the root of both VTrees.
     * @param filter  a filter for the result
     * @return count of variables that occurring in {@code vtree} and
     * {@code filter} but not in {@code subtree}
     */
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
}
