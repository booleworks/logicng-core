package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import java.util.BitSet;

public final class VTreeLeaf extends VTree {
    private final int variable;

    private BitSet clausePosMask = null;
    private BitSet clauseNegMask = null;

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

    public BitSet getClausePosMask() {
        return clausePosMask;
    }

    public void setClausePosMask(final BitSet clausePosMask) {
        this.clausePosMask = clausePosMask;
    }

    public BitSet getClauseNegMask() {
        return clauseNegMask;
    }

    public void setClauseNegMask(final BitSet clauseNegMask) {
        this.clauseNegMask = clauseNegMask;
    }

    @Override
    public String toString() {
        return "(" + getId() + ": " + variable + " )";
    }
}
