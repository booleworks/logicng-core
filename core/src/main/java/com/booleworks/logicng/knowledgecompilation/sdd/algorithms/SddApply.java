package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.TreeSet;

public class SddApply {
    private SddApply() {
    }

    public static LngResult<SddNode> apply(final SddNode left, final SddNode right, final SddApplyOperation op,
                                           final VTreeRoot root, final Sdd sf,
                                           final ComputationHandler handler) {
        if (!handler.shouldResume(SimpleEvent.SDD_APPLY)) {
            return LngResult.canceled(SimpleEvent.SDD_APPLY);
        }
        if (left == right) {
            return LngResult.of(left);
        }
        if (left == sf.getNegationIfCached(right)) {
            return LngResult.of(op.zero(sf));
        }
        if (op.isZero(left) || op.isZero(right)) {
            return LngResult.of(op.zero(sf));
        }
        if (op.isOne(left)) {
            return LngResult.of(right);
        }
        if (op.isOne(right)) {
            return LngResult.of(left);
        }

        final SddNode cached = sf.lookupApplyComputation(left, right, op);
        if (cached != null) {
            return LngResult.of(cached);
        }

        final SddNode l;
        final SddNode r;
        if (root.getPosition(left.getVTree()) <= root.getPosition(right.getVTree())) {
            l = left;
            r = right;
        } else {
            l = right;
            r = left;
        }
        final Pair<VTree, VTreeRoot.CmpType> lca = root.cmpVTrees(l.getVTree(), r.getVTree());
        final LngResult<SddNode> result;
        switch (lca.getSecond()) {
            case EQUALS:
                result = sddApplyEqual(l, r, op, root, sf, handler);
                break;
            case LEFT_SUBTREE:
                result = sddApplyLeft(l, r, op, root, sf, handler);
                break;
            case RIGHT_SUBTREE:
                result = sddApplyRight(l, r, op, root, sf, handler);
                break;
            case INCOMPARABLE:
                result = sddApplyIncomparable(l, r, op, root, sf, handler);
                break;
            default:
                throw new RuntimeException("Unknown ApplyType");
        }
        if (result.isSuccess()) {
            sf.cacheApplyComputation(l, r, result.getResult(), op);
        }
        return result;
    }


    private static LngResult<SddNode> sddApplyEqual(final SddNode left, final SddNode right, final SddApplyOperation op,
                                                    final VTreeRoot root, final Sdd sf,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        final LngResult<TreeSet<SddElement>> newElements =
                SddMultiply.multiplyDecompositions(left.asDecomposition().getElements(),
                        right.asDecomposition().getElements(), op, root, sf, handler);
        if (!newElements.isSuccess()) {
            return LngResult.canceled(newElements.getCancelCause());
        }
        return Util.getNodeOfPartition(newElements.getResult(), root, sf, handler);
    }

    private static LngResult<SddNode> sddApplyLeft(final SddNode left, final SddNode right, final SddApplyOperation op,
                                                   final VTreeRoot root, final Sdd sf,
                                                   final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(left.getVTree()) < root.getPosition(right.getVTree());

        final TreeSet<SddElement> newElements = new TreeSet<>();
        final SddNode n = op == SddApplyOperation.CONJUNCTION ? left : sf.negate(left, root);
        Util.pushNewElement(sf.negate(n, root), op.zero(sf), newElements);
        for (final SddElement element : right.asDecomposition().getElements()) {
            final LngResult<SddNode> newPrime =
                    apply(element.getPrime(), n, SddApplyOperation.CONJUNCTION, root, sf, handler);
            if (!newPrime.isSuccess()) {
                return newPrime;
            }
            if (!newPrime.getResult().isFalse()) {
                Util.pushNewElement(newPrime.getResult(), element.getSub(), newElements);
            }
        }
        return Util.getNodeOfPartition(newElements, root, sf, handler);
    }

    private static LngResult<SddNode> sddApplyRight(final SddNode left, final SddNode right, final SddApplyOperation op,
                                                    final VTreeRoot root, final Sdd sf,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(left.getVTree()) < root.getPosition(right.getVTree());

        final TreeSet<SddElement> newElements = new TreeSet<>();
        for (final SddElement element : left.asDecomposition().getElements()) {
            final LngResult<SddNode> newSub = apply(element.getSub(), right, op, root, sf, handler);
            if (!newSub.isSuccess()) {
                return newSub;
            }
            Util.pushNewElement(element.getPrime(), newSub.getResult(), newElements);
        }
        return Util.getNodeOfPartition(newElements, root, sf, handler);
    }

    private static LngResult<SddNode> sddApplyIncomparable(final SddNode left, final SddNode right,
                                                           final SddApplyOperation op, final VTreeRoot root,
                                                           final Sdd sf, final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(left.getVTree()) < root.getPosition(right.getVTree());
        assert !root.isSubtree(left.getVTree(), right.getVTree());
        assert !root.isSubtree(right.getVTree(), left.getVTree());

        final SddNode leftNeg = sf.negate(left, root);
        final LngResult<SddNode> leftSub = apply(right, sf.verum(), op, root, sf, handler);
        if (!leftSub.isSuccess()) {
            return leftSub;
        }
        final LngResult<SddNode> leftNegSub = apply(right, sf.falsum(), op, root, sf, handler);
        if (!leftNegSub.isSuccess()) {
            return leftNegSub;
        }
        final TreeSet<SddElement> newElements = new TreeSet<>();
        Util.pushNewElement(left, leftSub.getResult(), newElements);
        Util.pushNewElement(leftNeg, leftNegSub.getResult(), newElements);
        return LngResult.of(sf.decomposition(newElements, root));
    }

    public static SddNode conjoinUnsafe(final SddNode left, final SddNode right, final VTreeRoot root,
                                        final Sdd sf) {
        assert left != null && right != null;
        if (left.isFalse() || right.isFalse()) {
            return sf.falsum();
        }
        if (left.isTrue()) {
            return right;
        }
        if (right.isTrue()) {
            return left;
        }

        final SddNode cached = sf.lookupApplyComputation(left, right, SddApplyOperation.CONJUNCTION);
        if (cached != null) {
            return cached;
        }

        final TreeSet<SddElement> newElements = new TreeSet<>();
        newElements.add(new SddElement(left, right));
        newElements.add(new SddElement(sf.negate(left, root), sf.falsum()));
        final SddNode newNode = sf.decomposition(newElements, root);
        sf.cacheApplyComputation(left, right, newNode, SddApplyOperation.CONJUNCTION);
        return newNode;
    }
}
