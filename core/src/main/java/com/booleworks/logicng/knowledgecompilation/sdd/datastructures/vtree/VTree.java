package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

public abstract class VTree {
    protected final int id;

    public VTreeInternal asInternal() {
        return (VTreeInternal) this;
    }

    public VTreeLeaf asLeaf() {
        return (VTreeLeaf) this;
    }

    public VTree(final int id) {
        this.id = id;
    }

    public abstract boolean isLeaf();

    public abstract VTree getFirst();

    public abstract VTree getLast();

    public int getId() {
        return id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof VTree)) {
            return false;
        }

        final VTree vTree = (VTree) o;
        return id == vTree.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
