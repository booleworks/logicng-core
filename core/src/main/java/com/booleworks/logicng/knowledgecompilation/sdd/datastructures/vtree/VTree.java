package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

public abstract class VTree {
    private final int id;

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
}
