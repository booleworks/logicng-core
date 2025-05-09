package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.Collections;
import java.util.SortedSet;

public class SddNodeDecomposition extends SddNode {
    private final SortedSet<SddElement> elements;

    public SddNodeDecomposition(final int id, final VTree vTree,
                                final SortedSet<SddElement> elements) {
        super(id, vTree);
        this.elements = elements;
    }

    public SortedSet<SddElement> getElements() {
        return Collections.unmodifiableSortedSet(elements);
    }

    @Override
    public boolean isTrivial() {
        return false;
    }

    @Override
    public boolean isTrue() {
        return false;
    }

    @Override
    public boolean isFalse() {
        return false;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isDecomposition() {
        return true;
    }

    @Override
    public String toString() {
        return "(" + id + ": " + elements + " )";
    }


}
