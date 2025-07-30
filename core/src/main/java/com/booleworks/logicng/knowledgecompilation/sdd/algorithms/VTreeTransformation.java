package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;

/**
 * A collection of functions for transforming VTrees.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VTreeTransformation {
    private VTreeTransformation() {
    }

    /**
     * Rotates the VTree at its root to the left, i.e. from {@code (a (b c))} to
     * {@code ((a b) c)}
     * <ul>
     * <li><i>Preconditions:</i> the VTree must be a right fragment.</li>
     * </ul>
     * @param vTree the vtree
     * @param sdd   the SDD container
     * @return A left rotated copy of {@code vtree}
     */
    public static VTree rotateLeft(final VTreeInternal vTree, final Sdd sdd) {
        assert VTreeUtil.isRightFragment(vTree);
        final VTree a = vTree.getLeft();
        final VTree b = ((VTreeInternal) vTree.getRight()).getLeft();
        final VTree c = ((VTreeInternal) vTree.getRight()).getRight();
        final VTree left = sdd.vTreeInternal(a, b);
        return sdd.vTreeInternal(left, c);
    }

    /**
     * Rotates the VTree at its root to the right, i.e. from {@code ((a b) c)} to
     * {@code (a (b c))}
     * <ul>
     * <li><i>Preconditions:</i> the VTree must be a left fragment.</li>
     * </ul>
     * @param vTree the vtree
     * @param sdd   the SDD container
     * @return A right rotated copy of {@code vtree}
     */
    public static VTree rotateRight(final VTreeInternal vTree, final Sdd sdd) {
        assert VTreeUtil.isLeftFragment(vTree);
        final VTree a = ((VTreeInternal) vTree.getLeft()).getLeft();
        final VTree b = ((VTreeInternal) vTree.getLeft()).getRight();
        final VTree c = vTree.getRight();
        final VTree right = sdd.vTreeInternal(b, c);
        return sdd.vTreeInternal(a, right);
    }

    /**
     * Swaps the children of an internal VTree node.
     * <ul>
     * <li><i>Preconditions:</i> the VTree must not be a leaf.</li>
     * </ul>
     * @param vTree the vtree
     * @param sdd   the SDD container
     * @return A copy of {@code vtree} with swapped children
     */
    public static VTree swapChildren(final VTreeInternal vTree, final Sdd sdd) {
        assert !vTree.isLeaf();
        return sdd.vTreeInternal(vTree.getRight(), vTree.getLeft());
    }
}
