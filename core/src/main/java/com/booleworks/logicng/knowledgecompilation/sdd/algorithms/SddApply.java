package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;

public class SddApply {
    private SddApply() {
    }

    public static LngResult<SddNode> apply(final SddNode left, final SddNode right, final SddApplyOperation op,
                                           final Sdd sf, final ComputationHandler handler) {
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

        final SddNode l;
        final SddNode r;
        if (sf.vTreeOf(left).getPosition() <= sf.vTreeOf(right).getPosition()) {
            l = left;
            r = right;
        } else {
            l = right;
            r = left;
        }
        final Pair<VTree, VTreeRoot.CmpType> lca = sf.getVTree().cmpVTrees(sf.vTreeOf(l), sf.vTreeOf(r));
        final LngResult<SddNode> result;
        switch (lca.getSecond()) {
            case EQUALS:
                result = sddApplyEqual(l.asDecomposition(), r.asDecomposition(), op, sf, handler);
                break;
            case LEFT_SUBTREE:
                result = sddApplyLeft(l, r.asDecomposition(), op, sf, handler);
                break;
            case RIGHT_SUBTREE:
                result = sddApplyRight(l.asDecomposition(), r, op, sf, handler);
                break;
            case INCOMPARABLE:
                result = sddApplyIncomparable(l, r, op, sf, handler);
                break;
            default:
                throw new RuntimeException("Unknown ApplyType");
        }
        return result;
    }


    private static LngResult<SddNode> sddApplyEqual(final SddNodeDecomposition left, final SddNodeDecomposition right,
                                                    final SddApplyOperation op, final Sdd sf,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        final LngResult<ArrayList<SddElement>> newElements =
                SddMultiply.multiplyDecompositions(left.getElementsUnsafe(), right.getElementsUnsafe(), op, sf,
                        handler);
        if (!newElements.isSuccess()) {
            return LngResult.canceled(newElements.getCancelCause());
        }
        return sf.decompOfPartition(newElements.getResult(), handler);
    }

    private static LngResult<SddNode> sddApplyLeft(final SddNode left, final SddNodeDecomposition right,
                                                   final SddApplyOperation op, final Sdd sf,
                                                   final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert sf.vTreeOf(left).getPosition() < sf.vTreeOf(right).getPosition();

        final ArrayList<SddElement> newElements = new ArrayList<>();
        final SddNode n = op == SddApplyOperation.CONJUNCTION ? left : sf.negate(left);
        newElements.add(new SddElement(sf.negate(n), op.zero(sf)));
        for (final SddElement element : right) {
            final LngResult<SddNode> newPrime =
                    apply(element.getPrime(), n, SddApplyOperation.CONJUNCTION, sf, handler);
            if (!newPrime.isSuccess()) {
                return newPrime;
            }
            if (!newPrime.getResult().isFalse()) {
                newElements.add(new SddElement(newPrime.getResult(), element.getSub()));
            }
        }
        return sf.decompOfPartition(newElements, handler);
    }

    private static LngResult<SddNode> sddApplyRight(final SddNodeDecomposition left, final SddNode right,
                                                    final SddApplyOperation op, final Sdd sf,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert sf.vTreeOf(left).getPosition() < sf.vTreeOf(right).getPosition();

        final ArrayList<SddElement> newElements = new ArrayList<>();
        for (final SddElement element : left) {
            final LngResult<SddNode> newSub = apply(element.getSub(), right, op, sf, handler);
            if (!newSub.isSuccess()) {
                return newSub;
            }
            newElements.add(new SddElement(element.getPrime(), newSub.getResult()));
        }
        return sf.decompOfPartition(newElements, handler);
    }

    private static LngResult<SddNode> sddApplyIncomparable(final SddNode left, final SddNode right,
                                                           final SddApplyOperation op, final Sdd sf,
                                                           final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert sf.vTreeOf(left).getPosition() < sf.vTreeOf(right).getPosition();
        assert !sf.getVTree().isSubtree(sf.vTreeOf(left), sf.vTreeOf(right));
        assert !sf.getVTree().isSubtree(sf.vTreeOf(right), sf.vTreeOf(left));

        final SddNode leftNeg = sf.negate(left);
        final LngResult<SddNode> leftSub = apply(right, sf.verum(), op, sf, handler);
        if (!leftSub.isSuccess()) {
            return leftSub;
        }
        final LngResult<SddNode> leftNegSub = apply(right, sf.falsum(), op, sf, handler);
        if (!leftNegSub.isSuccess()) {
            return leftNegSub;
        }
        final ArrayList<SddElement> newElements = new ArrayList<>();
        newElements.add(new SddElement(left, leftSub.getResult()));
        newElements.add(new SddElement(leftNeg, leftNegSub.getResult()));
        return LngResult.of(sf.decompOfCompressedPartition(newElements));
    }
}
