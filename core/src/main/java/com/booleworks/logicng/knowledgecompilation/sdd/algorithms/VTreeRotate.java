package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;

public class VTreeRotate {
    private VTreeRotate() {
    }

    /**
     * Rotates (a (b c)) to ((a b) c)
     */
    public static VTree rotateLeft(final VTreeInternal vTree, final Sdd sf) {
        assert vTree.getRight() instanceof VTreeInternal;
        final VTree a = vTree.getLeft();
        final VTree b = ((VTreeInternal) vTree.getRight()).getLeft();
        final VTree c = ((VTreeInternal) vTree.getRight()).getRight();
        final VTree left = sf.vTreeInternal(a, b);
        return sf.vTreeInternal(left, c);
    }

    /**
     * Rotates ((a b) c) to (a (b c))
     */
    public static VTree rotateRight(final VTreeInternal vTree, final Sdd sf) {
        assert vTree.getLeft() instanceof VTreeInternal;
        final VTree a = ((VTreeInternal) vTree.getLeft()).getLeft();
        final VTree b = ((VTreeInternal) vTree.getLeft()).getRight();
        final VTree c = vTree.getRight();
        final VTree right = sf.vTreeInternal(b, c);
        return sf.vTreeInternal(a, right);
    }
}
