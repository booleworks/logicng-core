package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

public class VTreeInternal extends VTree {
    private final VTree left;
    private final VTree right;
    private final VTree first;
    private final VTree last;

    public VTreeInternal(final int id, final VTree left, final VTree right) {
        super(id);
        this.left = left;
        this.right = right;
        this.first = left.getFirst();
        this.last = right.getLast();
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
