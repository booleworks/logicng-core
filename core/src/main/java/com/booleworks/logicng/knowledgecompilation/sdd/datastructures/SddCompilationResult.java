package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

public class SddCompilationResult {
    private final Sdd sdd;
    private final SddNode node;

    public SddCompilationResult(final SddNode node, final Sdd sdd) {
        this.node = node;
        this.sdd = sdd;
    }

    public Sdd getSdd() {
        return sdd;
    }

    public SddNode getNode() {
        return node;
    }
}
