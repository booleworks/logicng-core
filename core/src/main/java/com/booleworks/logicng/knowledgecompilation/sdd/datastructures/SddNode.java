package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddVariablesFunction;

import java.util.SortedSet;

public abstract class SddNode implements Comparable<SddNode> {
    protected final int id;
    private final Sdd.GSCacheEntry<VTree> vTree;
    private Sdd.GSCacheEntry<SddNode> negation;
    private Sdd.VSCacheEntry<Integer> size;

    SddNode(final int id, final Sdd.GSCacheEntry<VTree> vTree) {
        this.id = id;
        this.vTree = vTree;
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

    void setSizeEntry(final Sdd.VSCacheEntry<Integer> size) {
        this.size = size;
    }

    Sdd.VSCacheEntry<Integer> getSizeEntry() {
        return size;
    }

    public SortedSet<Integer> variables() {
        return SddUtil.variables(this);
    }

    public SortedSet<Variable> variables(final Sdd sdd) {
        return execute(new SddVariablesFunction(sdd));
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
