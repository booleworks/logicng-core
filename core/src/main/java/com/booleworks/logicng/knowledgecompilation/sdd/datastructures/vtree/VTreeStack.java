package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;

import java.util.ArrayList;

public final class VTreeStack {
    private final ArrayList<VTreeRoot> vTrees;
    private int version;
    private int generation;

    public VTreeStack() {
        this.vTrees = new ArrayList<>();
        version = 0;
        generation = 0;
    }

    public VTreeRoot getActive() {
        assert !isEmpty();
        return vTrees.get(vTrees.size() - 1);
    }

    public int getVersion() {
        return version;
    }

    public int getGeneration() {
        return generation;
    }

    public void initialize(final VTreeRoot vTree) {
        vTrees.add(vTree);
        updateVTreeCaches();
    }

    public void push(final VTreeRoot vTree) {
        vTrees.add(vTree);
        updateVTreeCaches();
        version += 1;
        generation += 1;
    }

    public void bumpGeneration() {
        generation += 1;
        version += 1;
    }

    public void pop() {
        assert !vTrees.isEmpty();
        unpinAll();
        vTrees.remove(vTrees.size() - 1);
        if (!vTrees.isEmpty()) {
            updateVTreeCaches();
        }
    }

    public void stashTop() {
        assert !vTrees.isEmpty();
        vTrees.remove(vTrees.size() - 1);
        updateVTreeCaches();
    }

    public void removeInactive(final int count) {
        final VTreeRoot active = getActive();
        stashTop();
        for (int i = 0; !isEmpty() && i < count; i++) {
            pop();
        }
        push(active);
    }

    public void pin(final SddNodeDecomposition node) {
        version += 1;
        getActive().pin(node);
    }

    public void unpin(final SddNodeDecomposition node) {
        version += 1;
        getActive().unpin(node);
    }

    public void unpinAll() {
        version += 1;
        getActive().unpinAll();
    }

    public boolean isEmpty() {
        return vTrees.isEmpty();
    }

    public int size() {
        return vTrees.size();
    }

    private void updateVTreeCaches() {
        updatePositions(getActive().getRoot(), 0);
        updateParents(getActive().getRoot(), null);
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

}
