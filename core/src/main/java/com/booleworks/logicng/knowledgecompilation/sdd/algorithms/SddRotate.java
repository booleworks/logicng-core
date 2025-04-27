package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeShadow;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class SddRotate {
    private SddRotate() {
    }

    public static LngResult<Pair<SddNode, VTreeShadow>> rotateRight(final SddNode node, final VTreeInternal vtree,
                                                                    final VTreeShadow root, final SddFactory sf,
                                                                    final ComputationHandler handler) {
        if (!VTreeUtil.isLeftFragment(vtree)) {
            throw new IllegalArgumentException("Expected left linear vtree fragment for right rotate");
        }
        final VTreeShadow rotatedRoot = root.transform(VTreeOperation.ROTATE_RIGHT, vtree, sf);
        final VTreeInternal leftInternal = vtree.getLeft().asInternal();
        final LngResult<SddNode> rotated =
                rotateRightRecursive(node, vtree, leftInternal, root.getCurrent(), rotatedRoot.getCurrent(), sf,
                        handler, new HashMap<>());
        if (!rotated.isSuccess()) {
            rotatedRoot.rollback(sf);
            return LngResult.canceled(rotated.getCancelCause());
        } else {
            return LngResult.of(new Pair<>(rotated.getResult(), rotatedRoot));
        }
    }

    public static LngResult<Pair<SddNode, VTreeShadow>> rotateLeft(final SddNode node, final VTreeInternal vtree,
                                                                   final VTreeShadow root, final SddFactory sf,
                                                                   final ComputationHandler handler) {
        if (!VTreeUtil.isRightFragment(vtree)) {
            throw new IllegalArgumentException("Expected right linear vtree fragment for left rotate");
        }
        final VTreeShadow rotatedRoot = root.transform(VTreeOperation.ROTATE_LEFT, vtree, sf);
        final VTreeInternal rightInternal = vtree.getRight().asInternal();
        final LngResult<SddNode> rotated =
                rotateLeftRecursive(node, vtree, rightInternal, root.getCurrent(), rotatedRoot.getCurrent(), sf,
                        handler, new HashMap<>());
        if (!rotated.isSuccess()) {
            rotatedRoot.rollback(sf);
            return LngResult.canceled(rotated.getCancelCause());
        } else {
            return LngResult.of(new Pair<>(rotated.getResult(), rotatedRoot));
        }
    }

    private static LngResult<SddNode> rotateRightRecursive(final SddNode node, final VTreeInternal parentInner,
                                                           final VTreeInternal leftInner, final VTreeRoot root,
                                                           final VTreeRoot newRoot,
                                                           final SddFactory sf,
                                                           final ComputationHandler handler,
                                                           final Map<SddNode, SddNode> cache) {
        final SddNode cached = cache.get(node);
        if (cached != null) {
            return LngResult.of(cached);
        }
        if (node.isDecomposition()) {
            final VTree vt = root.getVTree(node);
            if (vt == parentInner) {
                final LngResult<TreeSet<SddElement>> rotatedElements =
                        rotateRightPartition(node.asDecomposition(), leftInner, root, newRoot, sf, handler);
                if (!rotatedElements.isSuccess()) {
                    return LngResult.canceled(rotatedElements.getCancelCause());
                }
                final LngResult<SddNode> newNode =
                        Util.getNodeOfPartition(rotatedElements.getResult(), newRoot, sf, handler);
                if (!newNode.isSuccess()) {
                    return newNode;
                }
                cache.put(node, newNode.getResult());
                return LngResult.of(newNode.getResult());
            } else if (root.isSubtree(vt, parentInner)) {
                sf.deepRegisterNode(node, newRoot);
                return LngResult.of(node);
            } else {
                final boolean moveInPrime = root.isSubtree(parentInner, vt.asInternal().getLeft());
                final boolean moveInSub = root.isSubtree(parentInner, vt.asInternal().getRight());
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    SddNode prime = element.getPrime();
                    SddNode sub = element.getSub();
                    if (moveInPrime && !element.getPrime().isTrivial()
                            && root.isSubtree(parentInner, root.getVTree(element.getPrime()))) {
                        final LngResult<SddNode> res =
                                rotateRightRecursive(element.getPrime(), parentInner, leftInner, root, newRoot,
                                        sf,
                                        handler, cache);
                        if (!res.isSuccess()) {
                            return res;
                        }
                        prime = res.getResult();
                    } else {
                        sf.deepRegisterNode(element.getPrime(), newRoot);
                    }
                    if (moveInSub && !element.getSub().isTrivial()
                            && root.isSubtree(parentInner, root.getVTree(element.getSub()))) {
                        final LngResult<SddNode> res =
                                rotateRightRecursive(element.getSub(), parentInner, leftInner, root, newRoot, sf,
                                        handler, cache);
                        if (!res.isSuccess()) {
                            return res;
                        }
                        sub = res.getResult();
                    } else {
                        sf.deepRegisterNode(element.getSub(), newRoot);
                    }
                    if (sub == element.getSub() && prime == element.getPrime()) {
                        elements.add(element);
                    } else {
                        elements.add(new SddElement(prime, sub));
                    }
                }
                final SddNode newNode = sf.decomposition(elements, newRoot);
                cache.put(node, newNode);
                return LngResult.of(newNode);
            }
        } else {
            final SddNode newNode = sf.terminal((Literal) node.asTerminal().getTerminal(), newRoot);
            cache.put(node, newNode);
            return LngResult.of(newNode);
        }
    }

    private static LngResult<TreeSet<SddElement>> rotateRightPartition(final SddNodeDecomposition node,
                                                                       final VTreeInternal leftInner,
                                                                       final VTreeRoot root, final VTreeRoot newRoot,
                                                                       final SddFactory sf,
                                                                       final ComputationHandler handler) {
        final ArrayList<TreeSet<SddElement>> sets = new ArrayList<>();
        for (final SddElement e1 : node.getElements()) {
            final TreeSet<SddElement> newElements = new TreeSet<>();
            sf.deepRegisterNode(e1.getPrime(), newRoot);
            sf.deepRegisterNode(e1.getSub(), newRoot);
            if (root.getVTree(e1.getPrime()) == leftInner) {
                for (final SddElement e2 : e1.getPrime().asDecomposition().getElements()) {
                    final SddNode bc = SddApply.conjoinUnsafe(e2.getSub(), e1.getSub(), newRoot, sf);
                    newElements.add(new SddElement(e2.getPrime(), bc));
                }
            } else if (root.isSubtree(root.getVTree(e1.getPrime()), leftInner.getRight())) {
                final SddNode a = sf.verum();
                final SddNode bc = SddApply.conjoinUnsafe(e1.getPrime(), e1.getSub(), newRoot, sf);
                newElements.add(new SddElement(a, bc));
            } else { //root.isSubTree(root.getVTree(e1.getPrime()), leftInner.getLeft())
                final SddNode a = e1.getPrime();
                final SddNode bc = e1.getSub();
                newElements.add(new SddElement(a, bc));
                final SddNode aNeg = sf.negate(a, newRoot);
                newElements.add(new SddElement(aNeg, sf.falsum()));
            }
            sets.add(newElements);
        }
        return SddCartesianProduct.cartesianProduct(sets, true, newRoot, sf, handler);
    }

    private static LngResult<SddNode> rotateLeftRecursive(final SddNode node, final VTreeInternal parentInner,
                                                          final VTreeInternal rightInner, final VTreeRoot root,
                                                          final VTreeRoot newRoot, final SddFactory sf,
                                                          final ComputationHandler handler,
                                                          final Map<SddNode, SddNode> cache) {
        final SddNode cached = cache.get(node);
        if (cached != null) {
            return LngResult.of(cached);
        }
        if (node.isDecomposition()) {
            final VTree vt = root.getVTree(node);
            if (vt == parentInner) {
                final LngResult<SddNode> rotated =
                        rotateLeftPartition(node.asDecomposition(), rightInner, root, newRoot, sf, handler);
                if (!rotated.isSuccess()) {
                    return LngResult.canceled(rotated.getCancelCause());
                }
                cache.put(node, rotated.getResult());
                return LngResult.of(rotated.getResult());
            } else if (root.isSubtree(vt, parentInner)) {
                sf.deepRegisterNode(node, newRoot);
                return LngResult.of(node);
            } else {
                final boolean moveInPrime = root.isSubtree(parentInner, vt.asInternal().getLeft());
                final boolean moveInSub = root.isSubtree(parentInner, vt.asInternal().getRight());
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    SddNode prime = element.getPrime();
                    SddNode sub = element.getSub();
                    if (moveInPrime && !element.getPrime().isTrivial()
                            && root.isSubtree(parentInner, root.getVTree(element.getPrime()))) {
                        final LngResult<SddNode> res =
                                rotateLeftRecursive(element.getPrime(), parentInner, rightInner, root, newRoot,
                                        sf, handler, cache);
                        if (!res.isSuccess()) {
                            return res;
                        }
                        prime = res.getResult();
                    } else {
                        sf.deepRegisterNode(element.getPrime(), newRoot);
                    }
                    if (moveInSub && !element.getSub().isTrivial()
                            && root.isSubtree(parentInner, root.getVTree(element.getSub()))) {
                        final LngResult<SddNode> res =
                                rotateLeftRecursive(element.getSub(), parentInner, rightInner, root, newRoot, sf,
                                        handler, cache);
                        if (!res.isSuccess()) {
                            return res;
                        }
                        sub = res.getResult();
                    } else {
                        sf.deepRegisterNode(element.getSub(), newRoot);
                    }
                    if (sub == element.getSub() && prime == element.getPrime()) {
                        elements.add(element);
                    } else {
                        elements.add(new SddElement(prime, sub));
                    }
                }
                final SddNode newNode = sf.decomposition(elements, newRoot);
                cache.put(node, newNode);
                return LngResult.of(newNode);
            }
        } else {
            final SddNode newNode = sf.terminal((Literal) node.asTerminal().getTerminal(), newRoot);
            cache.put(node, newNode);
            return LngResult.of(newNode);
        }
    }

    private static LngResult<SddNode> rotateLeftPartition(final SddNodeDecomposition node,
                                                          final VTreeInternal rightInner,
                                                          final VTreeRoot root, final VTreeRoot newRoot,
                                                          final SddFactory sf,
                                                          final ComputationHandler handler) {
        final TreeSet<SddElement> newElements = new TreeSet<>();
        for (final SddElement e1 : node.getElements()) {
            sf.deepRegisterNode(e1.getPrime(), newRoot);
            sf.deepRegisterNode(e1.getSub(), newRoot);
            if (e1.getSub().isTrivial()) {
                newElements.add(e1);
            } else if (root.getVTree(e1.getSub()) == rightInner) {
                for (final SddElement e2 : e1.getSub().asDecomposition().getElements()) {
                    final SddNode ab = SddApply.conjoinUnsafe(e1.getPrime(), e2.getPrime(), newRoot, sf);
                    newElements.add(new SddElement(ab, e2.getSub()));
                }
            } else if (root.getPosition(root.getVTree(e1.getSub())) > root.getPosition(rightInner)) {
                newElements.add(e1);
            } else {
                final SddNode ab = SddApply.conjoinUnsafe(e1.getPrime(), e1.getSub(), newRoot, sf);
                newElements.add(new SddElement(ab, sf.verum()));
                final SddNode bNeg = sf.negate(e1.getSub(), newRoot);
                final SddNode abNeg = SddApply.conjoinUnsafe(e1.getPrime(), bNeg, newRoot, sf);
                newElements.add(new SddElement(abNeg, sf.falsum()));
            }
        }
        return Util.getNodeOfPartition(newElements, newRoot, sf, handler);
    }
}
