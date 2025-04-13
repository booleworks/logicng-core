package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.TreeSet;

public class SddApply {
    private SddApply() {
    }

    public static LngResult<SddNode> apply(final SddNode left, final SddNode right, final SddApplyOperation op,
                                           final VTreeRoot root, final SddFactory sf,
                                           final ComputationHandler handler) {
        if (!handler.shouldResume(SimpleEvent.SDD_APPLY)) {
            LngResult.canceled(SimpleEvent.SDD_APPLY);
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
        if (root.getPosition(root.getVTree(left)) <= root.getPosition(root.getVTree(right))) {
            l = left;
            r = right;
        } else {
            l = right;
            r = left;
        }
        final Pair<VTree, VTreeRoot.CmpType> lca = root.cmpVTrees(root.getVTree(l), root.getVTree(r));
        final LngResult<SddNode> result;
        switch (lca.getSecond()) {
            case EQUALS:
                result = sddApplyEqual(l, r, op, lca.getFirst(), root, sf, handler);
                break;
            case LEFT_SUBTREE:
                result = sddApplyLeft(l, r, op, lca.getFirst(), root, sf, handler);
                break;
            case RIGHT_SUBTREE:
                result = sddApplyRight(l, r, op, lca.getFirst(), root, sf, handler);
                break;
            case INCOMPARABLE:
                result = sddApplyIncomparable(l, r, op, lca.getFirst(), root, sf, handler);
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
                                                    final VTree vTree, final VTreeRoot root, final SddFactory sf,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getVTree(left) == vTree;
        assert root.getVTree(right) == vTree;
        final LngResult<TreeSet<SddElement>> newElements =
                SddMultiply.multiplyDecompositions(left.asDecomposition().getElements(),
                        right.asDecomposition().getElements(), op, vTree, root, sf, handler);
        if (!newElements.isSuccess()) {
            return LngResult.canceled(newElements.getCancelCause());
        }
        return getNodeOfPartition(newElements.getResult(), vTree, root, sf, handler);
    }

    private static LngResult<SddNode> sddApplyLeft(final SddNode left, final SddNode right, final SddApplyOperation op,
                                                   final VTree vTree, final VTreeRoot root, final SddFactory sf,
                                                   final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(root.getVTree(left)) < root.getPosition(root.getVTree(right));
        assert root.getVTree(right) == vTree;
        assert root.isSubtree(root.getVTree(left), ((VTreeInternal) vTree).getLeft());

        final TreeSet<SddElement> newElements = new TreeSet<>();
        final SddNode n = op == SddApplyOperation.CONJUNCTION ? left : sf.negate(left, root);
        Util.pushNewElement(sf.negate(n, root), op.zero(sf), vTree, root, newElements);
        for (final SddElement element : right.asDecomposition().getElements()) {
            final LngResult<SddNode> newPrime =
                    apply(element.getPrime(), n, SddApplyOperation.CONJUNCTION, root, sf, handler);
            if (!newPrime.isSuccess()) {
                return newPrime;
            }
            if (!newPrime.getResult().isFalse()) {
                Util.pushNewElement(newPrime.getResult(), element.getSub(), vTree, root, newElements);
            }
        }
        return getNodeOfPartition(newElements, vTree, root, sf, handler);
    }

    private static LngResult<SddNode> sddApplyRight(final SddNode left, final SddNode right, final SddApplyOperation op,
                                                    final VTree vTree, final VTreeRoot root, final SddFactory sf,
                                                    final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(root.getVTree(left)) < root.getPosition(root.getVTree(right));
        assert root.getVTree(left) == vTree;
        assert root.isSubtree(root.getVTree(right), ((VTreeInternal) vTree).getRight());

        final TreeSet<SddElement> newElements = new TreeSet<>();
        for (final SddElement element : left.asDecomposition().getElements()) {
            final LngResult<SddNode> newSub = apply(element.getSub(), right, op, root, sf, handler);
            if (!newSub.isSuccess()) {
                return newSub;
            }
            Util.pushNewElement(element.getPrime(), newSub.getResult(), vTree, root, newElements);
        }
        return getNodeOfPartition(newElements, vTree, root, sf, handler);
    }

    private static LngResult<SddNode> sddApplyIncomparable(final SddNode left, final SddNode right,
                                                           final SddApplyOperation op,
                                                           final VTree vTree, final VTreeRoot root, final SddFactory sf,
                                                           final ComputationHandler handler) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(root.getVTree(left)) < root.getPosition(root.getVTree(right));
        assert root.isSubtree(root.getVTree(left), vTree);
        assert root.isSubtree(root.getVTree(right), vTree);
        assert !root.isSubtree(root.getVTree(left), root.getVTree(right));
        assert !root.isSubtree(root.getVTree(right), root.getVTree(left));

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
        Util.pushNewElement(left, leftSub.getResult(), vTree, root, newElements);
        Util.pushNewElement(leftNeg, leftNegSub.getResult(), vTree, root, newElements);
        return LngResult.of(sf.decomposition(newElements, root));
    }

    private static LngResult<SddNode> getNodeOfPartition(final TreeSet<SddElement> newElements, final VTree vTree,
                                                         final VTreeRoot root,
                                                         final SddFactory sf, final ComputationHandler handler) {
        final LngResult<Pair<SddNode, TreeSet<SddElement>>> res =
                compressAndTrim(newElements, vTree, root, sf, handler);
        if (!res.isSuccess()) {
            LngResult.canceled(res.getCancelCause());
        }
        if (res.getResult().getFirst() != null) {
            return LngResult.of(res.getResult().getFirst());
        } else {
            return LngResult.of(sf.decomposition(res.getResult().getSecond(), root));
        }
    }

    private static LngResult<Pair<SddNode, TreeSet<SddElement>>> compressAndTrim(final TreeSet<SddElement> elements,
                                                                                 final VTree vTree,
                                                                                 final VTreeRoot root,
                                                                                 final SddFactory sf,
                                                                                 final ComputationHandler handler) {
        assert !elements.isEmpty();

        final SddNode firstSub = elements.first().getSub();
        final SddNode lastSub = elements.last().getSub();

        if (firstSub == lastSub) {
            return LngResult.of(new Pair<>(firstSub, null));
        }

        // Trimming rule: node has form prime.T + ~prime.F, return prime
        if (firstSub.isTrue() && lastSub.isFalse()) {
            SddNode prime = sf.falsum();
            for (final SddElement element : elements) {
                if (!element.getSub().isTrue()) {
                    break;
                }
                final LngResult<SddNode> primeRes =
                        apply(element.getPrime(), prime, SddApplyOperation.DISJUNCTION, root, sf, handler);
                if (!primeRes.isSuccess()) {
                    return LngResult.canceled(primeRes.getCancelCause());
                }
                prime = primeRes.getResult();
                assert !prime.isTrivial();
                assert root.isSubtree(root.getVTree(prime), ((VTreeInternal) vTree).getLeft());
            }
            return LngResult.of(new Pair<>(prime, null));
        }

        //no trimming
        //pop uncompressed elements, compressing and placing compressed elements on element_stack
        SddNode cPrime = elements.first().getPrime();
        SddNode cSub = elements.first().getSub();
        SddElement cElement = elements.first();
        assert root.isOkPrimeIn(cPrime, vTree);
        final TreeSet<SddElement> compressedElements = new TreeSet<>();
        boolean first = true;
        for (final SddElement element : elements) {
            if (first) {
                first = false;
                continue;
            }
            assert root.isOkPrimeIn(element.getPrime(), vTree);
            assert root.isOkSubIn(element.getSub(), vTree);
            if (element.getSub() == cSub) { //compress
                final LngResult<SddNode> cPrimeRes =
                        apply(element.getPrime(), cPrime, SddApplyOperation.DISJUNCTION, root, sf, handler);
                if (!cPrimeRes.isSuccess()) {
                    return LngResult.canceled(cPrimeRes.getCancelCause());
                }
                cPrime = cPrimeRes.getResult();
                cElement = null;
                assert root.isOkPrimeIn(cPrime, vTree);
            } else {
                assert root.isOkPrimeIn(cPrime, vTree);
                assert root.isOkSubIn(cSub, vTree);
                if (cElement == null) {
                    compressedElements.add(new SddElement(cPrime, cSub));
                } else {
                    compressedElements.add(cElement);
                }
                cPrime = element.getPrime();
                cSub = element.getSub();
                cElement = element;
            }
        }
        if (cElement == null) {
            compressedElements.add(new SddElement(cPrime, cSub));
        } else {
            compressedElements.add(cElement);
        }
        return LngResult.of(new Pair<>(null, compressedElements));
    }
}
