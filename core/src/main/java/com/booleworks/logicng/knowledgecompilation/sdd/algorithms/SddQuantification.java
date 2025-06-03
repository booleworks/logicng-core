package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SddQuantification {
    public static LngResult<SddNode> existsSingle(final int var, final SddNode node,
                                                  final Sdd sf, final ComputationHandler handler) {
        final LngResult<SddNode> pCond = SddRestrict.restrict(var, true, node, sf, handler);
        if (!pCond.isSuccess()) {
            return pCond;
        }
        final LngResult<SddNode> nCond = SddRestrict.restrict(var, false, node, sf, handler);
        if (!nCond.isSuccess()) {
            return nCond;
        }
        return sf.disjunction(pCond.getResult(), nCond.getResult(), handler);
    }

    public static LngResult<SddNode> forallSingle(final int var, final SddNode node,
                                                  final Sdd sf, final ComputationHandler handler) {
        final LngResult<SddNode> pCond = SddRestrict.restrict(var, true, node, sf, handler);
        if (!pCond.isSuccess()) {
            return pCond;
        }
        final LngResult<SddNode> nCond = SddRestrict.restrict(var, false, node, sf, handler);
        if (!nCond.isSuccess()) {
            return nCond;
        }
        return sf.conjunction(pCond.getResult(), nCond.getResult(), handler);
    }

    public static LngResult<SddNode> exists(final Set<Integer> vars, final SddNode node,
                                            final Sdd sf, final ComputationHandler handler) {
        if (node.isTrivial()) {
            return LngResult.of(node);
        }
        return existsRec(vars, node, sf, handler, new HashMap<>());
    }

    private static LngResult<SddNode> existsRec(final Set<Integer> vars, final SddNode node, final Sdd sf,
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
            for (final SddElement element : node.asDecomposition()) {
                final LngResult<SddNode> prime = existsRec(vars, element.getPrime(), sf, handler, cache);
                if (!prime.isSuccess()) {
                    return prime;
                }
                final LngResult<SddNode> sub = existsRec(vars, element.getSub(), sf, handler, cache);
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

            final ArrayList<SddElement> newElements = getQuantifiedElements(node.asDecomposition(), cache);
            if (isPartition) {
                final LngResult<SddNode> newNode = sf.decompOfPartition(newElements, handler);
                if (!newNode.isSuccess()) {
                    return newNode;
                }
                cache.put(node, newNode.getResult());
                return newNode;
            } else {
                LngResult<SddNode> newNode = LngResult.of(sf.falsum());
                for (final SddElement element : newElements) {
                    final LngResult<SddNode> e = sf.conjunction(element.getPrime(), element.getSub(), handler);
                    if (!e.isSuccess()) {
                        return e;
                    }
                    newNode = sf.disjunction(e.getResult(), newNode.getResult(), handler);
                    if (!newNode.isSuccess()) {
                        return newNode;
                    }
                }
                cache.put(node, newNode.getResult());
                return newNode;
            }
        }
    }

    private static ArrayList<SddElement> getQuantifiedElements(final SddNodeDecomposition node,
                                                               final Map<SddNode, SddNode> cache) {
        final ArrayList<SddElement> quantified = new ArrayList<>();
        for (final SddElement element : node) {
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
