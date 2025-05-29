package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.collections.LngIntVector;

import java.util.ArrayList;

public class VTreeStack {
    private final ArrayList<VTreeRoot> vTrees;
    private final LngIntVector generationSteps;
    private int version;
    private int generation;
    private int minGeneration;

    public VTreeStack() {
        this.vTrees = new ArrayList<>();
        this.generationSteps = new LngIntVector();
        version = 0;
        generation = 0;
        minGeneration = 0;
    }

    public VTreeRoot getActive() {
        assert !isEmpty();
        return vTrees.get(vTrees.size() - 1);
    }

    public int getVersion() {
        return version;
    }

    public int getGeneration() {
        return generation;
    }

    public int getMinGeneration() {
        return minGeneration;
    }

    public LngIntVector getGenerationSteps() {
        return generationSteps;
    }

    public void initialize(final VTreeRoot vTree) {
        vTrees.add(vTree);
    }

    public void push(final VTreeRoot vTree) {
        vTrees.add(vTree);
        version += 1;
    }

    public void bumpGeneration() {
        generationSteps.push(version);
        generation += 1;
    }

    public void invalidateOldGenerations() {
        minGeneration = generation;
    }

    public void pop() {
        assert !vTrees.isEmpty();
        getActive().unpinAll();
        vTrees.remove(vTrees.size() - 1);
    }

    public void stashTop() {
        assert !vTrees.isEmpty();
        vTrees.remove(vTrees.size() - 1);
    }

    public void removeInactive(final int count) {
        final VTreeRoot active = getActive();
        stashTop();
        for (int i = 0; !isEmpty() && i < count; i++) {
            pop();
        }
        push(active);
    }

    public boolean isEmpty() {
        return vTrees.isEmpty();
    }
}
