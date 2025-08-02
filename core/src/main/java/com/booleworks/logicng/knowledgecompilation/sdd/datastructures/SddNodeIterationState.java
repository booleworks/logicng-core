package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import java.util.Iterator;

public class SddNodeIterationState {
    private final SddNodeDecomposition parent;
    private Iterator<SddElement> iterator;
    private SddElement current;
    private int generation = 0;

    public SddNodeIterationState(final SddNodeDecomposition parent) {
        this.parent = parent;
        reset();
    }

    public void reset() {
        this.iterator = parent.iterator();
        this.current = next();
    }

    public SddElement next() {
        if (!iterator.hasNext()) {
            current = null;
            return null;
        }
        current = iterator.next();
        if (current.getSub().isFalse()) {
            current = null;
            return next();
        }
        return current;
    }

    public SddElement getActiveElement() {
        return current;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(final int generation) {
        this.generation = generation;
    }
}
