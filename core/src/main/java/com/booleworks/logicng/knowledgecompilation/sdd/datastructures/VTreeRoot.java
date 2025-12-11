// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCoreSolver;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A vtree root.
 * <p>
 * A vtree root can no longer be extended or edited. It stores additional
 * information related to references counting of nodes that are normalized
 * over nodes of this vtree.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VTreeRoot {
    private final VTree root;
    private final ArrayList<SddNode> pinnedNodes;
    private final HashMap<SddNode, Integer> pinCount;
    private final HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes;
    private final HashMap<Integer, VTreeLeaf> leafVTreeNodes;
    private final SddVariableProxy variables;
    private int version;

    private VTreeRoot(final Builder builder, final VTree root) {
        this.root = root;
        this.pinnedNodes = new ArrayList<>();
        this.pinCount = new HashMap<>();
        this.version = 0;
        this.internalVTreeNodes = builder.internalVTreeNodes;
        this.leafVTreeNodes = builder.leafVTreeNodes;
        this.variables = builder.variables;
        initializeVTreeCaches();
    }

    /**
     * Returns an existing vtree leaf for the given variable or {@code null} if
     * no leaf does exist for the variable.
     * @param variable the variable
     * @return the vtree leaf for the variable or {@code null} if no leaf does
     * exist for the variable
     */
    public VTreeLeaf getVTreeLeaf(final Variable variable) {
        final int idx = variables.variableToIndex(variable);
        if (idx == -1) {
            return null;
        }
        return getVTreeLeaf(idx);
    }

    /**
     * Returns an existing vtree leaf for the given variable or {@code null} if
     * no leaf does exist for the variable.
     * @param variable the variable index
     * @return the vtree leaf for the variable or {@code null} if no leaf does
     * exist for the variable
     */
    public VTreeLeaf getVTreeLeaf(final int variable) {
        return leafVTreeNodes.get(variable);
    }

    /**
     * Returns an existing inner vtree node for a left and right subtree or
     * {@code null} if no node does exist.
     * @param left  the left subtree
     * @param right the right subtree
     * @return the inner vtree node or {@code null} if it does not exist
     */
    public VTreeInternal getVTreeInternal(final VTree left, final VTree right) {
        final Pair<VTree, VTree> pair = new Pair<>(left, right);
        return internalVTreeNodes.get(pair);
    }

    /**
     * Pins a node to this vtree root.
     * <p>
     * Pinned nodes and their children are not garbage collect as long as this
     * root is within the stack of the SDD container.
     * <p>
     * <strong>Do not use this function!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#pin Sdd.pin()}
     * instead of this function.
     * @param node the node
     */
    public void pin(final SddNodeDecomposition node) {
        final Integer count = pinCount.get(node);
        if (count == null) {
            version += 1;
            pinnedNodes.add(node);
            pinCount.put(node, 1);
            node.ref();
        } else {
            pinCount.put(node, pinCount.get(node) + 1);
        }
    }

    /**
     * Unpins a node from this vtree root.
     * <p>
     * The node and its children can now be removed by garbage collection if no
     * other pinned node references them.
     * <p>
     * <strong>Do not use this function!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#unpin Sdd.unpin()}
     * instead of this function.
     * @param node the node
     */
    public void unpin(final SddNodeDecomposition node) {
        final Integer count = pinCount.get(node);
        assert count != null;
        if (count == 1) {
            version += 1;
            pinnedNodes.remove(node);
            pinCount.remove(node);
            node.asDecomposition().deref();
        } else {
            pinCount.put(node, pinCount.get(node) - 1);
        }
    }

    /**
     * Unpins all nodes from this vtree root.
     * <p>
     * The node and its children can now be removed by garbage collection if no
     * other pinned node (from another root) references them.
     * <p>
     * <strong>Do not use this function!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#unpin Sdd.unpinAll()}
     * instead of this function.
     */
    public void unpinAll() {
        for (final SddNode pinnedNode : pinnedNodes) {
            pinnedNode.asDecomposition().deref();
        }
        pinnedNodes.clear();
        pinCount.clear();
    }

    /**
     * Returns whether {@code subtree} is a subtree of {@code of}.
     * @param subtree the subtree
     * @param of      the potential parent
     * @return whether {@code subtree} is a subtree of {@code of}
     */
    public boolean isSubtree(final VTree subtree, final VTree of) {
        return subtree.getPosition() >= of.getFirst().getPosition() && subtree.getPosition() <= of.getLast()
                .getPosition();
    }

    /**
     * Computes the lowest common ancestor of two vtrees.
     * @param vTree1 the first vtree
     * @param vTree2 the secornd vtree
     * @return the lowest common ancestor of the two vtrees
     */
    public VTree lcaOf(final VTree vTree1, final VTree vTree2) {
        if (vTree1 == vTree2) {
            return vTree1;
        } else if (vTree1 == null) {
            return vTree2;
        } else if (vTree2 == null) {
            return vTree1;
        } else if (vTree1.getParent() == vTree2.getParent()) {
            return vTree1.getParent();
        }

        final int p1 = vTree1.getPosition();
        final int p2 = vTree2.getPosition();
        VTree root = getRoot();
        while (true) {
            final int p = root.getPosition();
            if (p1 < p && p2 < p) {
                root = root.asInternal().getLeft();
            } else if (p1 > p && p2 > p) {
                root = root.asInternal().getRight();
            } else {
                return root;
            }
        }
    }

    /**
     * Computes the lowest common ancestor of two vtrees based on their position
     * in the vtree root.
     * @param posMin the smaller position index
     * @param posMax the larger position index
     * @return the lowest common ancestor of two vtree positions
     */
    public VTree lcaOf(final int posMin, final int posMax) {
        VTree current = getRoot();
        while (true) {
            if (current.isLeaf()) {
                assert current.getPosition() == posMin;
                assert posMin == posMax;
                return current;
            }
            if (current.asInternal().getLeft().getFirst().getPosition() <= posMin
                    && current.asInternal().getLeft().getLast().getPosition() >= posMax) {
                current = current.asInternal().getLeft();
            } else if (current.asInternal().getRight().getFirst().getPosition() <= posMin
                    && current.asInternal().getRight().getLast().getPosition() >= posMax) {
                current = current.asInternal().getRight();
            } else {
                return current;
            }
        }
    }

    /**
     * Computes how two vtrees relate to each other and computes the lowest
     * common ancestor of both vtrees.
     * <p>
     * Possible relations are:
     * <ul>
     *     <li>Equals: Both vtrees are the same vtree</li>
     *     <li>Left Subtree: The first vtree is subtree of the second vtree</li>
     *     <li>Right Subtree: The second vtree is subtree of the first vtree</li>
     *     <li>Incomparable: No vtree is a subtree of the other vtree</li>
     * </ul>
     * @param vtree1 the left vtree
     * @param vtree2 the right vtree
     * @return the relation and the lowest common ancestor of both vtrees
     */
    public Pair<VTree, CmpType> cmpVTrees(final VTree vtree1, final VTree vtree2) {
        assert vtree1.getPosition() <= vtree2.getPosition();

        if (vtree1 == vtree2) {
            return new Pair<>(vtree1, CmpType.EQUALS);
        } else if (vtree1.getPosition() >= vtree2.getFirst().getPosition()) {
            return new Pair<>(vtree2, CmpType.LEFT_SUBTREE);
        } else if (vtree2.getPosition() <= vtree1.getLast().getPosition()) {
            return new Pair<>(vtree1, CmpType.RIGHT_SUBTREE);
        } else {
            VTree lca = vtree1.getParent();
            while (vtree2.getPosition() > lca.getLast().getPosition()) {
                lca = lca.getParent();
            }
            return new Pair<>(lca, CmpType.INCOMPARABLE);
        }
    }

    public enum CmpType {
        /**
         * First and second vtree are identical.
         */
        EQUALS,
        /**
         * First vtree is a subtree of the second vtree.
         */
        LEFT_SUBTREE,
        /**
         * Second vtree is a subtree of the first vtree.
         */
        RIGHT_SUBTREE,
        /**
         * Vtrees are incomparable
         */
        INCOMPARABLE
    }

    /**
     * Returns the root vtree node.
     * @return the root vtree node
     */
    public VTree getRoot() {
        return root;
    }

    SddVariableProxy getVariables() {
        return variables;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Returns the pinned nodes of this root.
     * @return the pinned nodes of this root
     */
    public List<SddNode> getPinnedNodes() {
        return pinnedNodes;
    }

    @Override
    public String toString() {
        return "VTreeRoot{" +
                "root=" + root +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof VTreeRoot)) {
            return false;
        }

        final VTreeRoot vTreeRoot = (VTreeRoot) o;
        return root.equals(vTreeRoot.root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    private void initializeVTreeCaches() {
        if (root != null) {
            updatePositions(root, 0);
            updateParents(root, null);
        }
    }

    private int updatePositions(final VTree vTree, final int base) {
        if (vTree instanceof VTreeInternal) {
            final int b = updatePositions(((VTreeInternal) vTree).getLeft(), base);
            vTree.setPosition(b + 1);
            return updatePositions(((VTreeInternal) vTree).getRight(), b + 2);
        } else {
            vTree.setPosition(base);
            return base;
        }
    }

    private void updateParents(final VTree vTree, final VTree parent) {
        vTree.setParent(parent);
        if (!vTree.isLeaf()) {
            updateParents(vTree.asInternal().getLeft(), vTree);
            updateParents(vTree.asInternal().getRight(), vTree);
        }
    }

    /**
     * Constructs a new vtree builder that reuses the variable indices of a
     * solver. The builder does not add new variables or variable indices
     * to the solver.
     * @param solver the solver
     * @return the builder
     */
    public static Builder builderFromSolver(final SddCoreSolver solver) {
        return new Builder(SddVariableProxy.fromSolver(solver));
    }

    /**
     * Constructs a new vtree builder.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder(SddVariableProxy.empty());
    }

    /**
     * The vtree builder. Used to construct new vtrees.
     */
    public static class Builder {
        private int currentVTreeId = 0;
        private HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes = new HashMap<>();
        private HashMap<Integer, VTreeLeaf> leafVTreeNodes = new HashMap<>();
        private SddVariableProxy variables;

        private Builder(final SddVariableProxy variables) {
            this.variables = variables;
        }

        /**
         * Constructs a vtree leaf storing the variable or returns the existing
         * leaf if it already exists.
         * @param variable the variable
         * @return the vtree leaf for the variable
         */
        public VTreeLeaf vTreeLeaf(final Variable variable) {
            int idx = variables.variableToIndex(variable);
            if (idx == -1) {
                idx = variables.newVar(variable);
            }
            return vTreeLeaf(idx);
        }

        /**
         * Constructs a vtree leaf storing the variable or returns the existing
         * leaf if it already exists.
         * @param variable the variable index
         * @return the vtree leaf for the variable
         */
        public VTreeLeaf vTreeLeaf(final int variable) {
            final VTreeLeaf cached = leafVTreeNodes.get(variable);
            if (cached != null) {
                return cached;
            }
            final VTreeLeaf newNode = new VTreeLeaf(currentVTreeId++, variable);
            leafVTreeNodes.put(variable, newNode);
            return newNode;
        }

        /**
         * Constructs an inner vtree node from two vtree nodes or returns the
         * existing node if it already exists.
         * @param left  the left subtree
         * @param right the right subtree
         * @return the new inner vtree node
         */
        public VTreeInternal vTreeInternal(final VTree left, final VTree right) {
            final Pair<VTree, VTree> pair = new Pair<>(left, right);
            final VTreeInternal cached = internalVTreeNodes.get(pair);
            if (cached != null) {
                return cached;
            }
            final VTreeInternal newNode = new VTreeInternal(currentVTreeId++, left, right);
            internalVTreeNodes.put(pair, newNode);
            return newNode;
        }

        /**
         * Builds the {@link VTreeRoot} from this builder. The fields of this
         * builder are moved into the root. The builder can no longer be used
         * after calling this method.
         * @param rootNode the root node of the vtree
         * @return the built root
         */
        public VTreeRoot build(final VTree rootNode) {
            final VTreeRoot root = new VTreeRoot(this, rootNode);
            variables = null;
            internalVTreeNodes = null;
            leafVTreeNodes = null;
            currentVTreeId = 0;
            return root;
        }

        /**
         * Returns the container storing the variable to variable index
         * mappings.
         * @return the container storing the variable to variable index mappings
         */
        public SddVariableProxy getVariables() {
            return variables;
        }
    }
}
