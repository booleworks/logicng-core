package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.collections.LngIntVector;

import java.util.ArrayList;

public class VTreeStack {
    private final ArrayList<VTreeRoot> vTrees;
    private final LngIntVector levels;
    private int nextLevel;

    public VTreeStack() {
        this.vTrees = new ArrayList<>();
        this.levels = new LngIntVector();
        nextLevel = 0;
    }

    public VTreeRoot getActive() {
        assert !isEmpty();
        return vTrees.get(vTrees.size() - 1);
    }

    public int getActiveLevel() {
        assert !isEmpty();
        return levels.get(levels.size() - 1);
    }

    public VTreeRoot get(final int level) {
        for (int i = vTrees.size() - 1; i >= 0; --i) {
            if (levels.get(i) <= level) {
                return vTrees.get(i);
            }
        }
        return null;
    }

    public void initialize(final VTreeRoot vTree) {
        vTrees.add(vTree);
        levels.push(nextLevel++);
    }

    public void push(final VTreeRoot vTree) {
        vTrees.add(vTree);
        levels.push(nextLevel++);
    }

    public void pop() {
        assert !vTrees.isEmpty();
        assert !levels.isEmpty();
        getActive().unpinAll();
        vTrees.remove(vTrees.size() - 1);
        levels.removeElements(1);
    }

    public void stashTop() {
        assert !vTrees.isEmpty();
        assert !levels.isEmpty();
        vTrees.remove(vTrees.size() - 1);
        levels.removeElements(1);
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
