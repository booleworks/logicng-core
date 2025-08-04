package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import java.util.BitSet;

/**
 * The implementation of an internal vtree node.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VTreeInternal extends VTree {
    private final VTree left;
    private final VTree right;
    private final VTree first;
    private final VTree last;
    private final BitSet variableMask;

    /**
     * <strong>Do not use this constructor to construct new nodes.</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#vTreeInternal(VTree, VTree)
     * Sdd.vTreeInternal()},
     * as it ensure necessary invariants.
     * @param id    the unique id of this node
     * @param left  the left child
     * @param right the right child
     */
    public VTreeInternal(final int id, final VTree left, final VTree right) {
        super(id);
        this.left = left;
        this.right = right;
        this.first = left.getFirst();
        this.last = right.getLast();
        variableMask = new BitSet();
        if (left.isLeaf()) {
            variableMask.set(left.asLeaf().getVariable());
        } else {
            variableMask.or(left.asInternal().getVariableMask());
        }
        if (right.isLeaf()) {
            variableMask.set(right.asLeaf().getVariable());
        } else {
            variableMask.or(right.asInternal().getVariableMask());
        }
    }

    /**
     * Returns the left child.
     * @return the left child
     */
    public VTree getLeft() {
        return left;
    }

    /**
     * Returns the right child.
     * @return the right child
     */
    public VTree getRight() {
        return right;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns whether this node is a shannon node, i.e., a node where the left
     * child is a leaf.
     * @return whether this node is a shannon node
     */
    public boolean isShannon() {
        return left.isLeaf();
    }

    /**
     * Returns a mask (based on the internal integer representation of the
     * variables) for all variables contained in this vtree.
     * @return a mask for all variables contained in this vtree
     */
    public BitSet getVariableMask() {
        return variableMask;
    }

    @Override
    public VTree getFirst() {
        return first;
    }

    @Override
    public VTree getLast() {
        return last;
    }

    @Override
    public String toString() {
        return "(" + getId() +
                ": " + left +
                ", " + right +
                " )";
    }
}
