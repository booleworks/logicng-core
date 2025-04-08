package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

public class SddCompilationResult {
    private final SddNode sdd;
    private final VTreeRoot vTree;

    public SddCompilationResult(final SddNode sdd, final VTreeRoot vTree) {
        this.sdd = sdd;
        this.vTree = vTree;
    }

    public SddNode getSdd() {
        return sdd;
    }

    public VTreeRoot getVTree() {
        return vTree;
    }
}
