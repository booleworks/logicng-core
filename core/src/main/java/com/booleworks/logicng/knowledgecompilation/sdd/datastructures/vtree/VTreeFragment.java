package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeRotate;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeSwap;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

public class VTreeFragment {
    private final VTreeFragment previous;
    private final VTreeRoot current;
    private final VTreeOperation operation;

    public VTreeFragment(final VTreeRoot current, final VTreeFragment previous, final VTreeOperation operation) {
        this.previous = previous;
        this.operation = operation;
        this.current = current;
    }

    public static VTreeFragment fromRoot(final VTreeRoot root) {
        return new VTreeFragment(root, null, null);
    }

    public VTreeFragment transform(final VTreeOperation operation, final VTreeInternal node, final SddFactory sf) {
        final VTree newNode;
        switch (operation) {
            case ROTATE_LEFT:
                newNode = VTreeRotate.rotateLeft(node, sf);
                break;
            case ROTATE_RIGHT:
                newNode = VTreeRotate.rotateRight(node, sf);
                break;
            case SWAP:
                newNode = VTreeSwap.swapChildren(node, sf);
                break;
            default:
                throw new RuntimeException("Unknown vtree operation");
        }
        final VTreeRoot newRoot = sf.constructRoot(VTreeUtil.substituteNode(current.getRoot(), node, newNode, sf));
        return new VTreeFragment(newRoot, this, operation);
    }

    public VTreeRoot apply(final SddFactory sf) {
        VTreeFragment prev = previous;
        while (prev != null) {
            sf.deregisterVTree(prev.getCurrent());
            prev = previous.getPrevious();
        }
        return this.getCurrent();
    }

    public VTreeFragment rollback(final SddFactory sf) {
        sf.deregisterVTree(current);
        return previous;
    }

    public VTreeRoot rollbackAll(final SddFactory sf) {
        VTreeFragment c = this;
        while (c.getPrevious() != null) {
            sf.deregisterVTree(c.getCurrent());
            c = c.getPrevious();
        }
        return c.getCurrent();
    }

    public VTreeFragment getPrevious() {
        return previous;
    }

    public VTreeOperation getOperation() {
        return operation;
    }

    public VTreeRoot getCurrent() {
        return current;
    }
}
