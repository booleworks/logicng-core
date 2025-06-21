package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.BitSet;
import java.util.Map;

public abstract class VTree {
    protected final int id;
    private VTree parent = null;
    private int position = -1;

    protected BitSet contextClauseMask = null;
    protected BitSet contextLitsInMask = null;
    protected Map<BitSet, Map<BitSet, SddNode>> cache;

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

    void setPosition(final int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public VTree getParent() {
        return parent;
    }

    void setParent(final VTree parent) {
        this.parent = parent;
    }

    public BitSet getContextClauseMask() {
        return contextClauseMask;
    }

    public void setContextClauseMask(final BitSet contextClauseMask) {
        this.contextClauseMask = contextClauseMask;
    }

    public BitSet getContextLitsInMask() {
        return contextLitsInMask;
    }

    public void setContextLitsInMask(final BitSet contextLitsInMask) {
        this.contextLitsInMask = contextLitsInMask;
    }

    public Map<BitSet, Map<BitSet, SddNode>> getCache() {
        return cache;
    }

    public void setCache(
            final Map<BitSet, Map<BitSet, SddNode>> cache) {
        this.cache = cache;
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
