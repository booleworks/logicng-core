package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction;

import java.util.BitSet;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SddNode implements Comparable<SddNode> {
    protected final int id;
    protected BitSet variableMask;
    private final Sdd.GSCacheEntry<VTree> vTree;
    private Sdd.GSCacheEntry<SddNode> negation;
    private Sdd.VSCacheEntry<Integer> size;

    SddNode(final int id, final Sdd.GSCacheEntry<VTree> vTree) {
        this.id = id;
        this.vTree = vTree;
        this.variableMask = null;
        this.negation = null;
        this.size = null;
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

    Sdd.GSCacheEntry<VTree> getVTreeEntry() {
        return vTree;
    }

    Sdd.GSCacheEntry<SddNode> getNegationEntry() {
        return negation;
    }

    void setNegationEntry(final Sdd.GSCacheEntry<SddNode> negation) {
        this.negation = negation;
    }

    public BitSet getVariableMask() {
        if (variableMask == null) {
            calculateVariableMask();
        }
        return variableMask;
    }

    void setSizeEntry(final Sdd.VSCacheEntry<Integer> size) {
        this.size = size;
    }

    Sdd.VSCacheEntry<Integer> getSizeEntry() {
        return size;
    }

    private void calculateVariableMask() {
        variableMask = new BitSet();
        if (isDecomposition()) {
            for (final SddElement element : asDecomposition()) {
                variableMask.or(element.getPrime().getVariableMask());
                variableMask.or(element.getSub().getVariableMask());
            }
        } else if (isLiteral()) {
            variableMask.set(asTerminal().getVTree().getVariable());
        }
    }

    public SortedSet<Integer> variables() {
        final TreeSet<Integer> variables = new TreeSet<>();
        final BitSet mask = getVariableMask();
        for (int i = mask.nextSetBit(0); i != -1; i = mask.nextSetBit(i + 1)) {
            variables.add(i);
        }
        return variables;
    }

    public <RESULT> RESULT execute(final SddFunction<RESULT> function) {
        return function.execute(this);
    }

    public <RESULT> LngResult<RESULT> execute(final SddFunction<RESULT> function, final ComputationHandler handler) {
        return function.execute(this, handler);
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
