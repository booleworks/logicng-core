package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import java.util.TreeSet;

public class SddNodeDecomposition extends SddNode {
    private final TreeSet<SddElement> elements;

    public SddNodeDecomposition(final int id, final TreeSet<SddElement> elements) {
        super(id);
        this.elements = elements;
    }

    public TreeSet<SddElement> getElements() {
        return elements;
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
