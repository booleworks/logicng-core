package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.BitSet;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SddNode implements Comparable<SddNode> {
    final protected int id;
    protected VTree vTree;
    protected final BitSet variableMask;
    protected SddNode negation = null;

    public SddNode(final int id, final VTree vTree, final BitSet variableMask) {
        this.id = id;
        this.vTree = vTree;
        this.variableMask = variableMask;
    }

    public int getId() {
        return id;
    }

    abstract public boolean isTrivial();

    abstract public boolean isTrue();

    abstract public boolean isFalse();

    abstract public boolean isLiteral();

    abstract public boolean isDecomposition();

    public SddNodeDecomposition asDecomposition() {
        return (SddNodeDecomposition) this;
    }

    public SddNodeTerminal asTerminal() {
        return (SddNodeTerminal) this;
    }

    public VTree getVTree() {
        return vTree;
    }

    public void updateVTree(final VTree vTree) {
        this.vTree = vTree;
    }

    SddNode getNegation() {
        return negation;
    }

    void setNegation(final SddNode negation) {
        this.negation = negation;
    }

    public BitSet getVariableMask() {
        return variableMask;
    }

    public SortedSet<Integer> variables() {
        final TreeSet<Integer> variables = new TreeSet<>();
        for (int i = variableMask.nextSetBit(0); i != -1; i = variableMask.nextSetBit(i + 1)) {
            variables.add(i);
        }
        return variables;
    }

    @Override
    public int compareTo(final SddNode o) {
        return id - o.id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof SddNode)) {
            return false;
        }

        final SddNode sddNode = (SddNode) o;
        return id == sddNode.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
