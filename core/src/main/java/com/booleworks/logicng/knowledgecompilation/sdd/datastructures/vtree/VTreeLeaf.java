package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;

public class VTreeLeaf extends VTree {
    private final Variable variable;

    public VTreeLeaf(final int id, final Variable variable) {
        super(id);
        this.variable = variable;
    }

    public Variable getVariable() {
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
