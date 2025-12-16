// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;

/**
 * Apply-Transformations for SDDs.
 * <p>
 * The function is intended to be used internally and implements the raw
 * algorithm.  If you want to perform this kind of operation, it is suggested
 * {@link Sdd#binaryOperation(SddNode, SddNode, Operation, ComputationHandler) Sdd.binaryOperation()}
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
     * {@link Sdd#binaryOperation(SddNode, SddNode, Operation, ComputationHandler) Sdd.binaryOperation()}
     * or one of its variants, as they do additional caching of already computed
     * operations.
     * @param sdd     the SDD container
     * @param left    the left operand
     * @param right   the right operand
     * @param op      a binary operation (conjunction or disjunction)
     * @param handler the computation handler
     * @return a new SDD which is the result of the binary operation or a
     * canceling cause if the computation was aborted.
     * @see Sdd#binaryOperation(SddNode, SddNode, Operation, ComputationHandler) Sdd.binaryOperation()
     * @see Sdd#conjunction(SddNode, SddNode, ComputationHandler) Sdd.conjunction()
     * @see Sdd#disjunction(SddNode, SddNode, ComputationHandler) Sdd.disjunction()
     */
    public static LngResult<SddNode> apply(final Sdd sdd, final SddNode left, final SddNode right, final Operation op,
                                           final ComputationHandler handler) {
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
        if (left.getVTree().getPosition() <= right.getVTree().getPosition()) {
            l = left;
            r = right;
        } else {
            l = right;
            r = left;
        }
        final Pair<VTree, VTreeRoot.CmpType> lca = sdd.getVTree().cmpVTrees(l.getVTree(), r.getVTree());
        final LngResult<SddNode> result;
        switch (lca.getSecond()) {
            case EQUALS:
                result = sddApplyEqual(sdd, l.asDecomposition(), r.asDecomposition(), op, handler);
                break;
            case LEFT_SUBTREE:
                result = sddApplyLeft(sdd, l, r.asDecomposition(), op, handler);
                break;
            case RIGHT_SUBTREE:
                result = sddApplyRight(sdd, l.asDecomposition(), r, op, handler);
                break;
            case INCOMPARABLE:
                result = sddApplyIncomparable(sdd, l, r, op, handler);
                break;
            default:
                throw new RuntimeException("Unknown ApplyType");
        }
        return result;
    }


    private static LngResult<SddNode> sddApplyEqual(final Sdd sdd, final SddNodeDecomposition left,
                                                    final SddNodeDecomposition right, final Operation op,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        final LngResult<ArrayList<SddElement>> newElements =
                SddMultiply.multiplyDecompositions(sdd, left.getElementsUnsafe(), right.getElementsUnsafe(), op,
                        handler);
        if (!newElements.isSuccess()) {
            return LngResult.canceled(newElements.getCancelCause());
        }
        return sdd.decompOfPartition(newElements.getResult(), handler);
    }

    private static LngResult<SddNode> sddApplyLeft(final Sdd sdd, final SddNode left, final SddNodeDecomposition right,
                                                   final Operation op, final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert left.getVTree().getPosition() < right.getVTree().getPosition();

        final ArrayList<SddElement> newElements = new ArrayList<>();
        final SddNode n = op == Operation.CONJUNCTION ? left : sdd.negate(left);
        newElements.add(new SddElement(sdd.negate(n), op.zero(sdd)));
        for (final SddElement element : right) {
            final LngResult<SddNode> newPrime =
                    apply(sdd, element.getPrime(), n, Operation.CONJUNCTION, handler);
            if (!newPrime.isSuccess()) {
                return newPrime;
            }
            if (!newPrime.getResult().isFalse()) {
                newElements.add(new SddElement(newPrime.getResult(), element.getSub()));
            }
        }
        return sdd.decompOfPartition(newElements, handler);
    }

    private static LngResult<SddNode> sddApplyRight(final Sdd sdd, final SddNodeDecomposition left, final SddNode right,
                                                    final Operation op, final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert left.getVTree().getPosition() < right.getVTree().getPosition();

        final ArrayList<SddElement> newElements = new ArrayList<>();
        for (final SddElement element : left) {
            final LngResult<SddNode> newSub = apply(sdd, element.getSub(), right, op, handler);
            if (!newSub.isSuccess()) {
                return newSub;
            }
            newElements.add(new SddElement(element.getPrime(), newSub.getResult()));
        }
        return sdd.decompOfPartition(newElements, handler);
    }

    private static LngResult<SddNode> sddApplyIncomparable(final Sdd sdd, final SddNode left, final SddNode right,
                                                           final Operation op, final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert left.getVTree().getPosition() < right.getVTree().getPosition();
        assert !sdd.getVTree().isSubtree(left.getVTree(), right.getVTree());
        assert !sdd.getVTree().isSubtree(right.getVTree(), left.getVTree());

        final SddNode leftNeg = sdd.negate(left);
        final LngResult<SddNode> leftSub = apply(sdd, right, sdd.verum(), op, handler);
        if (!leftSub.isSuccess()) {
            return leftSub;
        }
        final LngResult<SddNode> leftNegSub = apply(sdd, right, sdd.falsum(), op, handler);
        if (!leftNegSub.isSuccess()) {
            return leftNegSub;
        }
        final ArrayList<SddElement> newElements = new ArrayList<>();
        newElements.add(new SddElement(left, leftSub.getResult()));
        newElements.add(new SddElement(leftNeg, leftNegSub.getResult()));
        return LngResult.of(sdd.decompOfCompressedPartition(newElements));
    }

    public enum Operation {
        CONJUNCTION,
        DISJUNCTION;

        public SddNodeTerminal zero(final Sdd sf) {
            switch (this) {
                case CONJUNCTION:
                    return sf.falsum();
                case DISJUNCTION:
                    return sf.verum();
            }
            throw new RuntimeException("Unsupported operation");
        }

        public SddNodeTerminal one(final Sdd sf) {
            switch (this) {
                case CONJUNCTION:
                    return sf.verum();
                case DISJUNCTION:
                    return sf.falsum();
            }
            throw new RuntimeException("Unsupported operation");
        }

        public boolean isZero(final SddNode node) {
            switch (this) {
                case CONJUNCTION:
                    return node instanceof SddNodeTerminal
                            && node.isFalse();
                case DISJUNCTION:
                    return node instanceof SddNodeTerminal
                            && node.isTrue();
            }
            throw new RuntimeException("Unsupported operation");
        }

        public boolean isOne(final SddNode node) {
            switch (this) {
                case CONJUNCTION:
                    return node instanceof SddNodeTerminal
                            && node.isTrue();
                case DISJUNCTION:
                    return node instanceof SddNodeTerminal
                            && node.isFalse();
            }
            throw new RuntimeException("Unsupported operation");
        }
    }
}
