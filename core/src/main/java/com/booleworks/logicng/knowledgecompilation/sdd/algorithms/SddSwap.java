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
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class SddSwap {
    private SddSwap() {
    }

    public static LngResult<Pair<SddNode, VTreeShadow>> swap(final SddNode node, final VTreeInternal vTree,
                                                             final VTreeShadow root,
                                                             final SddFactory sf,
                                                             final ComputationHandler handler) {
        final VTreeShadow swappedRoot = root.transform(VTreeOperation.SWAP, vTree, sf);
        final LngResult<SddNode> swapped =
                swapRecursive(node, vTree, root.getCurrent(), swappedRoot.getCurrent(), sf, handler, new HashMap<>());
        if (!swapped.isSuccess()) {
            swappedRoot.rollback(sf);
            return LngResult.canceled(swapped.getCancelCause());
        } else {
            return LngResult.of(new Pair<>(swapped.getResult(), swappedRoot));
        }
    }

    private static LngResult<SddNode> swapRecursive(final SddNode node, final VTree vtree, final VTreeRoot root,
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
            if (vt == vtree) {
                final LngResult<TreeSet<SddElement>> elements =
                        swapPartition(node.asDecomposition(), newRoot, sf, handler);
                if (!elements.isSuccess()) {
                    return LngResult.canceled(elements.getCancelCause());
                }
                final SddNode newNode = sf.decomposition(elements.getResult(), newRoot);
                cache.put(node, newNode);
                return LngResult.of(newNode);
            } else {
                final boolean moveInPrime = root.isSubtree(vtree, vt.asInternal().getLeft());
                final boolean moveInSub = root.isSubtree(vtree, vt.asInternal().getRight());
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    SddNode prime = element.getPrime();
                    SddNode sub = element.getSub();
                    if (moveInPrime && !element.getPrime().isTrivial()
                            && root.isSubtree(vtree, root.getVTree(element.getPrime()))) {
                        final LngResult<SddNode> res =
                                swapRecursive(element.getPrime(), vtree, root, newRoot, sf, handler, cache);
                        if (!res.isSuccess()) {
                            return res;
                        }
                        prime = res.getResult();
                    } else {
                        sf.deepRegisterNode(element.getPrime(), newRoot);
                    }
                    if (moveInSub && !element.getSub().isTrivial()
                            && root.isSubtree(vtree, root.getVTree(element.getSub()))) {
                        final LngResult<SddNode> res =
                                swapRecursive(element.getSub(), vtree, root, newRoot, sf, handler, cache);
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

    private static LngResult<TreeSet<SddElement>> swapPartition(final SddNodeDecomposition node,
                                                                final VTreeRoot newRoot, final SddFactory sf,
                                                                final ComputationHandler handler) {
        final ArrayList<TreeSet<SddElement>> sets = new ArrayList<>();
        for (final SddElement element : node.getElements()) {
            sf.deepRegisterNode(element.getPrime(), newRoot);
            sf.deepRegisterNode(element.getSub(), newRoot);
            final SddNode negSub = sf.negate(element.getSub(), newRoot);
            final TreeSet<SddElement> newElements = new TreeSet<>();
            if (!element.getSub().isFalse()) {
                Util.pushNewElement(element.getSub(), element.getPrime(), newElements);
            }
            if (!negSub.isFalse()) {
                Util.pushNewElement(negSub, sf.falsum(), newElements);
            }
            sets.add(newElements);
        }
        return SddCartesianProduct.cartesianProduct(sets, false, newRoot, sf, handler);
    }
}
