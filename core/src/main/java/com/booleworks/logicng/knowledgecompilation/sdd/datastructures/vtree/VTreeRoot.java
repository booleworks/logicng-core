package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VTreeRoot {
    private final HashMap<VTree, VTreeInternal> parents;
    private final VTree root;
    private final HashMap<VTree, Integer> positions;
    private final HashMap<Integer, VTree> position2VTree;
    private final ArrayList<SddNode> pinnedNodes;
    private final HashMap<SddNode, Integer> pinCount;

    public VTreeRoot(final HashMap<VTree, VTreeInternal> parents, final VTree root,
                     final HashMap<VTree, Integer> positions, final HashMap<Integer, VTree> position2VTree) {
        this.parents = parents;
        this.root = root;
        this.positions = positions;
        this.position2VTree = position2VTree;
        this.pinnedNodes = new ArrayList<>();
        this.pinCount = new HashMap<>();
    }

    public void pin(final SddNode node) {
        final Integer count = pinCount.get(node);
        if (count == null) {
            pinnedNodes.add(node);
            pinCount.put(node, 1);
        } else {
            pinCount.put(node, pinCount.get(node) + 1);
        }
    }

    public void unpin(final SddNode node) {
        final Integer count = pinCount.get(node);
        assert count != null;
        if (count == 1) {
            pinnedNodes.remove(node);
            pinCount.remove(node);
        } else {
            pinCount.put(node, pinCount.get(node) - 1);
        }
    }

    public boolean isSubtree(final VTree subtree, final VTree of) {
        return getPosition(subtree) >= getPosition(of.getFirst()) && getPosition(subtree) <= getPosition(of.getLast());
    }

    public VTree lcaOf(final VTree vTree1, final VTree vTree2) {
        if (vTree1 == vTree2) {
            return vTree1;
        } else if (getParent(vTree1) == getParent(vTree2)) {
            return getParent(vTree1);
        }

        final int p1 = getPosition(vTree1);
        final int p2 = getPosition(vTree2);
        VTree root = getRoot();
        while (true) {
            final int p = getPosition(root);
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
                assert getPosition(current) == posMin;
                assert posMin == posMax;
                return current;
            }
            if (getPosition(current.asInternal().getLeft().getFirst()) <= posMin
                    && getPosition(current.asInternal().getLeft().getLast()) >= posMax) {
                current = current.asInternal().getLeft();
            } else if (getPosition(current.asInternal().getRight().getFirst()) <= posMin
                    && getPosition(current.asInternal().getRight().getLast()) >= posMax) {
                current = current.asInternal().getRight();
            } else {
                return current;
            }
        }
    }

    public Pair<VTree, CmpType> cmpVTrees(final VTree vtree1, final VTree vtree2) {
        assert getPosition(vtree1) <= getPosition(vtree2);

        if (vtree1 == vtree2) {
            return new Pair<>(vtree1, CmpType.EQUALS);
        } else if (getPosition(vtree1) >= getPosition(vtree2.getFirst())) {
            return new Pair<>(vtree2, CmpType.LEFT_SUBTREE);
        } else if (getPosition(vtree2) <= getPosition(vtree1.getLast())) {
            return new Pair<>(vtree1, CmpType.RIGHT_SUBTREE);
        } else {
            VTree lca = getParent(vtree1);
            while (getPosition(vtree2) > getPosition(lca.getLast())) {
                lca = getParent(lca);
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

    public VTreeInternal getParent(final VTree child) {
        return parents.get(child);
    }

    public HashMap<VTree, VTreeInternal> getParents() {
        return parents;
    }

    public VTree getRoot() {
        return root;
    }

    public int getPosition(final VTree vTree) {
        return positions.get(vTree);
    }

    public VTree getVTreeAtPosition(final int pos) {
        return position2VTree.get(pos);
    }

    public VTree getNext(final VTree vTree) {
        assert positions.containsKey(vTree);
        final int pos = getPosition(vTree);
        return getVTreeAtPosition(pos + 1);
    }

    public VTree getPrevious(final VTree vTree) {
        assert positions.containsKey(vTree);
        final int pos = getPosition(vTree);
        return getVTreeAtPosition(pos - 1);
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
