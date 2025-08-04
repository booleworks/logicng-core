package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
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

    /**
     * <strong>Do not use this constructor!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#constructRoot Sdd.constructRoot()}
     * or {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#defineVTree Sdd.defineVTree()}
     * instead.
     * @param root the vtree which is used to construct the root
     */
    public VTreeRoot(final VTree root) {
        this.root = root;
        this.pinnedNodes = new ArrayList<>();
        this.pinCount = new HashMap<>();
    }

    /**
     * Copy constructor for vtree roots.
     * <p>
     * <strong>Important:</strong> This API is unsafe you should now what you
     * are doing if you are using it.  The root is only valid on the SDD it was
     * constructed on or an identical copy. Furthermore, you need to ensure
     * that all pinned SDD nodes are still on the SDD (i.e. not garbage
     * collected. Ideally the original vtree is still on the SDD, which pins all
     * these nodes). To define the copied vtree for an SDD container, you need
     * to push it to the vtree stack and then bump the generation of the stack
     * (important, otherwise the cache gets corrupted).
     * <pre>{@code
     * VTreeRoot copy = new VTreeRoot(existing);
     * ...
     * sdd.getVTreeStack().push(copy);
     * sdd.bumpGeneration();
     * ...
     * }</pre>
     * @param root the existing root
     */
    public VTreeRoot(final VTreeRoot root) {
        this.root = root.root;
        this.pinnedNodes = new ArrayList<>(root.pinnedNodes);
        this.pinCount = new HashMap<>(root.pinCount);
        for (final SddNode pinnedNode : root.pinnedNodes) {
            pinnedNode.asDecomposition().ref();
        }
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
}
