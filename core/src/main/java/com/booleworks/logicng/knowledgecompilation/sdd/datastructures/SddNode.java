package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.BitSet;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SddNode implements Comparable<SddNode> {
    protected final int id;
    protected final BitSet variableMask;
    private final Sdd.CacheEntry<VTree> vTree;
    private Sdd.CacheEntry<SddNode> negation;

    SddNode(final int id, final Sdd.CacheEntry<VTree> vTree, final BitSet variableMask) {
        this.id = id;
        this.vTree = vTree;
        this.variableMask = variableMask;
        this.negation = null;
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

    Sdd.CacheEntry<VTree> getVTreeEntry() {
        return vTree;
    }

    Sdd.CacheEntry<SddNode> getNegationEntry() {
        return negation;
    }

    void setNegationEntry(final Sdd.CacheEntry<SddNode> negation) {
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
