package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

public class SddCompilationResult {
    private final Sdd sdd;
    private final SddNode node;
    private final VTreeRoot vTree;

    public SddCompilationResult(final SddNode node, final VTreeRoot vTree, final Sdd sdd) {
        this.node = node;
        this.sdd = sdd;
        this.vTree = vTree;
    }

    public Sdd getSdd() {
        return sdd;
    }

    public SddNode getNode() {
        return node;
    }

    public VTreeRoot getVTree() {
        return vTree;
    }
}
