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

/**
 * Apply-Transformations for SDDs.
 * <p>
 * The function is intended to be used internally and implements the raw
 * algorithm.  If you want to perform this kind of operation, it is suggested
 * {@link Sdd#binaryOperation(SddNode, SddNode, SddApplyOperation, ComputationHandler) Sdd.binaryOperation()}
 * or one of its variants, as they do additional caching of already computed
 * operations.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddApply {
    private SddApply() {
    }

    /**
     * Performs a binary operation on two SDD nodes.  The binary operation can
     * either be a conjunction or a disjunction.
     * <p>
     * The function is intended to be used internally and implements the raw
     * algorithm.  If you want to perform this kind of operation, it is suggested
     * {@link Sdd#binaryOperation(SddNode, SddNode, SddApplyOperation, ComputationHandler) Sdd.binaryOperation()}
     * or one of its variants, as they do additional caching of already computed
     * operations.
     * @param left    the left operand
     * @param right   the right operand
     * @param op      a binary operation (conjunction or disjunction)
     * @param sdd     the SDD container
     * @param handler the computation handler
     * @return a new SDD which is the result of the binary operation or a
     * canceling cause if the computation was aborted.
     * @see Sdd#binaryOperation(SddNode, SddNode, SddApplyOperation, ComputationHandler) Sdd.binaryOperation()
     * @see Sdd#conjunction(SddNode, SddNode, ComputationHandler) Sdd.conjunction()
     * @see Sdd#disjunction(SddNode, SddNode, ComputationHandler) Sdd.disjunction()
     */
    public static LngResult<SddNode> apply(final SddNode left, final SddNode right, final SddApplyOperation op,
                                           final Sdd sdd, final ComputationHandler handler) {
        if (!handler.shouldResume(SimpleEvent.SDD_APPLY)) {
            return LngResult.canceled(SimpleEvent.SDD_APPLY);
        }
        if (left == right) {
            return LngResult.of(left);
        }
        if (left == sdd.getNegationIfCached(right)) {
            return LngResult.of(op.zero(sdd));
        }
        if (op.isZero(left) || op.isZero(right)) {
            return LngResult.of(op.zero(sdd));
        }
        if (op.isOne(left)) {
            return LngResult.of(right);
        }
        if (op.isOne(right)) {
            return LngResult.of(left);
        }

        final SddNode l;
        final SddNode r;
        if (sdd.vTreeOf(left).getPosition() <= sdd.vTreeOf(right).getPosition()) {
            l = left;
            r = right;
        } else {
            l = right;
            r = left;
        }
        final Pair<VTree, VTreeRoot.CmpType> lca = sdd.getVTree().cmpVTrees(sdd.vTreeOf(l), sdd.vTreeOf(r));
        final LngResult<SddNode> result;
        switch (lca.getSecond()) {
            case EQUALS:
                result = sddApplyEqual(l.asDecomposition(), r.asDecomposition(), op, sdd, handler);
                break;
            case LEFT_SUBTREE:
                result = sddApplyLeft(l, r.asDecomposition(), op, sdd, handler);
                break;
            case RIGHT_SUBTREE:
                result = sddApplyRight(l.asDecomposition(), r, op, sdd, handler);
                break;
            case INCOMPARABLE:
                result = sddApplyIncomparable(l, r, op, sdd, handler);
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
