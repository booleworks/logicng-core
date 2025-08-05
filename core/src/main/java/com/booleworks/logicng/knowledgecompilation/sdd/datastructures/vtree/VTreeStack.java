package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;

import java.util.ArrayList;

/**
 * The vtree stack, a rollbackable history of vtree roots of an SDD container.
 * <p>
 * As the name suggest, it is a stack of vtree. The vtree at the top is referred
 * to as the "active vtree" and the other vtrees are "inactive vtrees".  The
 * user or global transformations can push new vtree roots to the stack if they
 * want to change the configuration of the SDD container and its nodes, and
 * can roll back to old vtree roots that are still on the stack.
 */
public final class VTreeStack {
    private final ArrayList<VTreeRoot> vTrees;
    private int version;
    private int generation;

    /**
     * Constructs a new empty vtree stack.
     */
    public VTreeStack() {
        this.vTrees = new ArrayList<>();
        version = 0;
        generation = 0;
    }

    /**
     * Returns the active vtree. A vtree must be defined.
     * @return the active vtree
     */
    public VTreeRoot getActive() {
        assert !isEmpty();
        return vTrees.get(vTrees.size() - 1);
    }

    /**
     * Returns the current version counter.
     * @return the current version counter
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the current generation counter.
     * @return the current generation counter
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * Puts the first vtree root on the stack
     * @param vTree the first vtree root
     */
    public void initialize(final VTreeRoot vTree) {
        vTrees.add(vTree);
        updateVTreeCaches();
    }

    /**
     * Adds a vtree root to the stack.
     * <p>
     * This will invalidate all generation-based and version-based caches.
     */
    public void push(final VTreeRoot vTree) {
        vTrees.add(vTree);
        updateVTreeCaches();
        version += 1;
        generation += 1;
    }

    /**
     * Bumps the generation counter by one.
     * <p>
     * This will invalidate all generation-based and version-based caches.
     */
    public void bumpGeneration() {
        generation += 1;
        version += 1;
    }

    /**
     * Removes the active vtree and unpins all its pinned nodes.
     * <p>
     * This will invalidate all version-based caches.
     */
    public void pop() {
        assert !vTrees.isEmpty();
        unpinAll();
        vTrees.remove(vTrees.size() - 1);
        if (!vTrees.isEmpty()) {
            updateVTreeCaches();
        }
    }

    /**
     * Removes the active vtree but does not unpin its nodes.
     * <p>
     * This will invalidate all version-based caches.
     */
    public void stashTop() {
        assert !vTrees.isEmpty();
        vTrees.remove(vTrees.size() - 1);
        updateVTreeCaches();
    }

    /**
     * Removes inactive vtree from top to bottom and unpins all the pinned ndoes
     * of the removed vtrees.
     * <p>
     * This will invalidate all version-based caches.
     * @param count the number of vtrees to remove
     */
    public void removeInactive(final int count) {
        final VTreeRoot active = getActive();
        stashTop();
        for (int i = 0; !isEmpty() && i < count; i++) {
            pop();
        }
        push(active);
    }

    /**
     * Pins a node to the active vtree.
     * <p>
     * <strong>Do not use this function!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#pin Sdd.pin()}
     * instead of this function.
     * <p>
     * This will invalidate all version-based caches.
     * @param node the node
     */
    public void pin(final SddNodeDecomposition node) {
        version += 1;
        getActive().pin(node);
    }

    /**
     * Unpins a node from the active vtree.
     * <p>
     * <strong>Do not use this function!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#unpin Sdd.unpin()}
     * instead of this function.
     * <p>
     * This will invalidate all version-based caches.
     * @param node the node
     */
    public void unpin(final SddNodeDecomposition node) {
        version += 1;
        getActive().unpin(node);
    }

    /**
     * Unpins all nodes from the active vtree.
     * <p>
     * <strong>Do not use this function!</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#unpin Sdd.unpinAll()}
     * instead of this function.
     * <p>
     * This will invalidate all version-based caches.
     */
    public void unpinAll() {
        version += 1;
        getActive().unpinAll();
    }

    /**
     * Returns whether there is currently an active vtree.
     * @return whether there is currently an active vtree
     */
    public boolean isEmpty() {
        return vTrees.isEmpty();
    }

    /**
     * Returns the number of vtrees on the stack.
     * @return the number of vtrees on the stack
     */
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
