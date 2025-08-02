package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import java.util.BitSet;

public final class VTreeInternal extends VTree {
    private final VTree left;
    private final VTree right;
    private final VTree first;
    private final VTree last;
    private final BitSet variableMask;

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

    public VTree getLeft() {
        return left;
    }

    public VTree getRight() {
        return right;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public boolean isShannon() {
        return left.isLeaf();
    }

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
