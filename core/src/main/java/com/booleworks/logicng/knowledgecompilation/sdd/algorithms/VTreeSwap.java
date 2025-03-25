package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;

public class VTreeSwap {
    private VTreeSwap() {
    }

    public static VTree swapChildren(final VTreeInternal vTree, final SddFactory sf) {
        return sf.vTreeInternal(vTree.getRight(), vTree.getLeft());
    }
}
