package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SddQuantification {
    public static LngResult<SddNode> existsSingle(final int var, final SddNode node, final VTreeRoot root,
                                                  final Sdd sf, final ComputationHandler handler) {
        final LngResult<SddNode> pCond = SddRestrict.restrict(var, true, node, root, sf, handler);
        if (!pCond.isSuccess()) {
            return pCond;
        }
        final LngResult<SddNode> nCond = SddRestrict.restrict(var, false, node, root, sf, handler);
        if (!nCond.isSuccess()) {
            return nCond;
        }
        return sf.disjunction(pCond.getResult(), nCond.getResult(), root, handler);
    }

    public static LngResult<SddNode> forallSingle(final int var, final SddNode node, final VTreeRoot root,
                                                  final Sdd sf, final ComputationHandler handler) {
        final LngResult<SddNode> pCond = SddRestrict.restrict(var, true, node, root, sf, handler);
        if (!pCond.isSuccess()) {
            return pCond;
        }
        final LngResult<SddNode> nCond = SddRestrict.restrict(var, false, node, root, sf, handler);
        if (!nCond.isSuccess()) {
            return nCond;
        }
        return sf.conjunction(pCond.getResult(), nCond.getResult(), root, handler);
    }

    public static LngResult<SddNode> exists(final Set<Integer> vars, final SddNode node, final VTreeRoot root,
                                            final Sdd sf, final ComputationHandler handler) {
        if (node.isTrivial()) {
            return LngResult.of(node);
        }
        return existsRec(vars, node, root, sf, handler, new HashMap<>());
    }

    private static LngResult<SddNode> existsRec(final Set<Integer> vars, final SddNode node,
                                                final VTreeRoot root, final Sdd sf,
                                                final ComputationHandler handler, final Map<SddNode, SddNode> cache) {
        final SddNode cached = cache.get(node);
        if (cached != null) {
            return LngResult.of(cached);
        }
        if (node.isTrivial()) {
            cache.put(node, node);
            return LngResult.of(node);
        } else if (node.isLiteral()) {
            final SddNodeTerminal t = node.asTerminal();
            if (vars.contains(t.getVTree().getVariable())) {
                cache.put(node, sf.verum());
                return LngResult.of(sf.verum());
            } else {
                cache.put(node, node);
                return LngResult.of(node);
            }
        } else {
            boolean isTrue = false;
            boolean isPartition = true;
            boolean isChanged = false;
            for (final SddElement element : node.asDecomposition().getElements()) {
                final LngResult<SddNode> prime = existsRec(vars, element.getPrime(), root, sf, handler, cache);
                if (!prime.isSuccess()) {
                    return prime;
                }
                final LngResult<SddNode> sub = existsRec(vars, element.getSub(), root, sf, handler, cache);
                if (!sub.isSuccess()) {
                    return sub;
                }
                isTrue |= prime.getResult().isTrue() && sub.getResult().isTrue();
                isPartition &= prime.getResult() == element.getPrime();
                isChanged |= !isPartition || sub.getResult() != element.getSub();
            }

            if (!isChanged) {
                cache.put(node, node);
                return LngResult.of(node);
            }
            if (isTrue) {
                cache.put(node, sf.verum());
                return LngResult.of(sf.verum());
            }

            final TreeSet<SddElement> newElements = getQuantifiedElements(node.asDecomposition(), cache);
            if (isPartition) {
                final LngResult<SddNode> newNode = Util.getNodeOfPartition(newElements, root, sf, handler);
                if (!newNode.isSuccess()) {
                    return newNode;
                }
                cache.put(node, newNode.getResult());
                return newNode;
            } else {
                LngResult<SddNode> newNode = LngResult.of(sf.falsum());
                for (final SddElement element : newElements) {
                    final LngResult<SddNode> e = sf.conjunction(element.getPrime(), element.getSub(), root, handler);
                    if (!e.isSuccess()) {
                        return e;
                    }
                    newNode = sf.disjunction(e.getResult(), newNode.getResult(), root, handler);
                    if (!newNode.isSuccess()) {
                        return newNode;
                    }
                }
                cache.put(node, newNode.getResult());
                return newNode;
            }
        }
    }

    private static TreeSet<SddElement> getQuantifiedElements(final SddNodeDecomposition node,
                                                             final Map<SddNode, SddNode> cache) {
        final TreeSet<SddElement> quantified = new TreeSet<>();
        for (final SddElement element : node.getElements()) {
            final SddNode p = cache.get(element.getPrime());
            final SddNode s = cache.get(element.getSub());
            assert p != null;
            assert s != null;
            if (p == element.getPrime() && s == element.getSub()) {
                quantified.add(element);
            } else {
                quantified.add(new SddElement(p, s));
            }
        }
        return quantified;
    }
}
