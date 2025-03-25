package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import org.jetbrains.annotations.NotNull;

public abstract class SddNode implements Comparable<SddNode> {
    final int id;

    public SddNode(final int id) {
        this.id = id;
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

    @Override
    public int compareTo(@NotNull final SddNode o) {
        return id - o.id;
    }
}
