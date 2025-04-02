package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

public abstract class SddNode implements Comparable<SddNode> {
    final protected int id;

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
