package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

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

    public static SddNode apply(final SddNode left, final SddNode right, final SddApplyOperation op,
                                final VTreeRoot root, final SddFactory sf) {
        if (left == right) {
            return left;
        }
        if (left == sf.getNegationIfCached(right)) {
            return op.zero(sf);
        }
        if (op.isZero(left) || op.isZero(right)) {
            return op.zero(sf);
        }
        if (op.isOne(left)) {
            return right;
        }
        if (op.isOne(right)) {
            return left;
        }

        final SddNode cached = sf.lookupApplyComputation(left, right, op);
        if (cached != null) {
            return cached;
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
        final SddNode result;
        switch (lca.getSecond()) {
            case EQUALS:
                result = sddApplyEqual(l, r, op, lca.getFirst(), root, sf);
                break;
            case LEFT_SUBTREE:
                result = sddApplyLeft(l, r, op, lca.getFirst(), root, sf);
                break;
            case RIGHT_SUBTREE:
                result = sddApplyRight(l, r, op, lca.getFirst(), root, sf);
                break;
            case INCOMPARABLE:
                result = sddApplyIncomparable(l, r, op, lca.getFirst(), root, sf);
                break;
            default:
                throw new RuntimeException("Unknown ApplyType");
        }
        sf.cacheApplyComputation(l, r, result, op);
        return result;
    }


    private static SddNode sddApplyEqual(final SddNode left, final SddNode right, final SddApplyOperation op,
                                         final VTree vTree, final VTreeRoot root, final SddFactory sf) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getVTree(left) == vTree;
        assert root.getVTree(right) == vTree;
        final TreeSet<SddElement> newElements =
                SddMultiply.multiplyDecompositions(left.asDecomposition().getElements(),
                        right.asDecomposition().getElements(), op, vTree, root, sf);
        return getNodeOfPartition(newElements, vTree, root, sf);
    }

    private static SddNode sddApplyLeft(final SddNode left, final SddNode right, final SddApplyOperation op,
                                        final VTree vTree, final VTreeRoot root, final SddFactory sf) {
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
            final SddNode newPrime = apply(element.getPrime(), n, SddApplyOperation.CONJUNCTION, root, sf);
            if (!newPrime.isFalse()) {
                Util.pushNewElement(newPrime, element.getSub(), vTree, root, newElements);
            }
        }
        return getNodeOfPartition(newElements, vTree, root, sf);
    }

    private static SddNode sddApplyRight(final SddNode left, final SddNode right, final SddApplyOperation op,
                                         final VTree vTree, final VTreeRoot root, final SddFactory sf) {
        assert left != null;
        assert right != null;
        assert !left.isTrivial();
        assert !right.isTrivial();
        assert root.getPosition(root.getVTree(left)) < root.getPosition(root.getVTree(right));
        assert root.getVTree(left) == vTree;
        assert root.isSubtree(root.getVTree(right), ((VTreeInternal) vTree).getRight());

        final TreeSet<SddElement> newElements = new TreeSet<>();
        for (final SddElement element : left.asDecomposition().getElements()) {
            final SddNode newSub = apply(element.getSub(), right, op, root, sf);
            Util.pushNewElement(element.getPrime(), newSub, vTree, root, newElements);
        }
        return getNodeOfPartition(newElements, vTree, root, sf);
    }

    private static SddNode sddApplyIncomparable(final SddNode left, final SddNode right, final SddApplyOperation op,
                                                final VTree vTree, final VTreeRoot root, final SddFactory sf) {
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
        final SddNode leftSub = apply(right, sf.verum(), op, root, sf);
        final SddNode leftNegSub = apply(right, sf.falsum(), op, root, sf);
        final TreeSet<SddElement> newElements = new TreeSet<>();
        Util.pushNewElement(left, leftSub, vTree, root, newElements);
        Util.pushNewElement(leftNeg, leftNegSub, vTree, root, newElements);
        return sf.decomposition(newElements, vTree, root);
    }

    private static SddNode getNodeOfPartition(final TreeSet<SddElement> newElements, final VTree vTree,
                                              final VTreeRoot root,
                                              final SddFactory sf) {
        final Pair<SddNode, TreeSet<SddElement>> res = compressAndTrim(newElements, vTree, root, sf);
        if (res.getFirst() != null) {
            return res.getFirst();
        } else {
            return sf.decomposition(res.getSecond(), vTree, root);
        }
    }

    private static Pair<SddNode, TreeSet<SddElement>> compressAndTrim(final TreeSet<SddElement> elements,
                                                                      final VTree vTree,
                                                                      final VTreeRoot root, final SddFactory sf) {
        assert !elements.isEmpty();

        final SddNode firstSub = elements.first().getSub();
        final SddNode lastSub = elements.last().getSub();

        if (firstSub == lastSub) {
            return new Pair<>(firstSub, null);
        }

        // Trimming rule: node has form prime.T + ~prime.F, return prime
        if (firstSub.isTrue() && lastSub.isFalse()) {
            SddNode prime = sf.falsum();
            for (final SddElement element : elements) {
                if (!element.getSub().isTrue()) {
                    break;
                }
                prime = apply(element.getPrime(), prime, SddApplyOperation.DISJUNCTION, root, sf);
                assert !prime.isTrivial();
                assert root.isSubtree(root.getVTree(prime), ((VTreeInternal) vTree).getLeft());
            }
            return new Pair<>(prime, null);
        }

        //no trimming
        //pop uncompressed elements, compressing and placing compressed elements on element_stack
        SddNode cPrime = elements.first().getPrime();
        SddNode cSub = elements.first().getSub();
        SddElement cElement = elements.first();
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
                cPrime = apply(element.getPrime(), cPrime, SddApplyOperation.DISJUNCTION, root, sf);
                cElement = null;
                assert root.isOkPrimeIn(element.getPrime(), vTree);
            } else {
                assert root.isOkPrimeIn(element.getPrime(), vTree);
                assert root.isOkSubIn(element.getSub(), vTree);
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
        return new Pair<>(null, compressedElements);
    }
}
