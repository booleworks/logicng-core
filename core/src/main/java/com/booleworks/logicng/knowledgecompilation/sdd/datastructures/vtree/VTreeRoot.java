package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class VTreeRoot {
    private final VTree root;
    private final ArrayList<SddNode> pinnedNodes;
    private final HashMap<SddNode, Integer> pinCount;

    public VTreeRoot(final VTree root) {
        this.root = root;
        this.pinnedNodes = new ArrayList<>();
        this.pinCount = new HashMap<>();
    }

    public VTreeRoot(final VTreeRoot root) {
        this.root = root.root;
        this.pinnedNodes = new ArrayList<>(root.pinnedNodes);
        this.pinCount = new HashMap<>(root.pinCount);
        for (final SddNode pinnedNode : root.pinnedNodes) {
            pinnedNode.asDecomposition().ref();
        }
    }

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

    public void unpinAll() {
        for (final SddNode pinnedNode : pinnedNodes) {
            pinnedNode.asDecomposition().deref();
        }
        pinnedNodes.clear();
        pinCount.clear();
    }

    public boolean isSubtree(final VTree subtree, final VTree of) {
        return subtree.getPosition() >= of.getFirst().getPosition() && subtree.getPosition() <= of.getLast()
                .getPosition();
    }

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
        /// first and second vtree are identical.
        EQUALS,
        /// first vtree is a subtree of the second vtree.
        LEFT_SUBTREE,
        /// second vtree is a subtree of the first vtree.
        RIGHT_SUBTREE,
        /// vtrees are incomparable
        INCOMPARABLE
    }

    public VTree getRoot() {
        return root;
    }

    public List<SddNode> getPinnedNodes() {
        return pinnedNodes;
    }

    public int getId() {
        return root.getId();
    }

    @Override
    public String toString() {
        return "VTreeRoot{" +
                "root=" + root +
                '}';
    }

    @Override
    public final boolean equals(final Object o) {
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
