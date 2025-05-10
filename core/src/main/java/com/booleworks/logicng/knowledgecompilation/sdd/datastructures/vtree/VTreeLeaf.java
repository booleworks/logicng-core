package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

public class VTreeLeaf extends VTree {
    private final int variable;

    public VTreeLeaf(final int id, final int variable) {
        super(id);
        this.variable = variable;
    }

    public int getVariable() {
        return variable;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public VTree getFirst() {
        return this;
    }

    @Override
    public VTree getLast() {
        return this;
    }

    @Override
    public String toString() {
        return "(" + getId() + ": " + variable + " )";
    }
}
