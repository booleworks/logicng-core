// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

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

/**
 * A collection of functions for quantifier elimination on SDDs.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddQuantification {
    private SddQuantification() {
    }

    /**
     * Computes existential quantifier elimination for a set of variables.
     * @param vars    the variables to eliminate
     * @param node    the SDD node
     * @param sdd     the SDD container of {@code node}
     * @param handler the computation handler
     * @return the SDD with the eliminated variables
     * @see com.booleworks.logicng.knowledgecompilation.sdd.functions.SddProjectionFunction SddProjectionFunction
     */
    public static LngResult<SddNode> exists(final Set<Integer> vars, final SddNode node,
                                            final Sdd sdd, final ComputationHandler handler) {
        if (node.isTrivial()) {
            return LngResult.of(node);
        }
        return existsRec(vars, node, sdd, handler, new HashMap<>());
    }

    private static LngResult<SddNode> existsRec(final Set<Integer> vars, final SddNode node, final Sdd sdd,
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
                cache.put(node, sdd.verum());
                return LngResult.of(sdd.verum());
            } else {
                cache.put(node, node);
                return LngResult.of(node);
            }
        } else {
            boolean isTrue = false;
            boolean isPartition = true;
            boolean isChanged = false;
            for (final SddElement element : node.asDecomposition()) {
                final LngResult<SddNode> prime = existsRec(vars, element.getPrime(), sdd, handler, cache);
                if (!prime.isSuccess()) {
                    return prime;
                }
                final LngResult<SddNode> sub = existsRec(vars, element.getSub(), sdd, handler, cache);
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
                cache.put(node, sdd.verum());
                return LngResult.of(sdd.verum());
            }

            final ArrayList<SddElement> newElements = getQuantifiedElements(node.asDecomposition(), cache);
            if (isPartition) {
                final LngResult<SddNode> newNode = sdd.decompOfPartition(newElements, handler);
                if (!newNode.isSuccess()) {
                    return newNode;
                }
                cache.put(node, newNode.getResult());
                return newNode;
            } else {
                LngResult<SddNode> newNode = LngResult.of(sdd.falsum());
                for (final SddElement element : newElements) {
                    final LngResult<SddNode> e = sdd.conjunction(element.getPrime(), element.getSub(), handler);
                    if (!e.isSuccess()) {
                        return e;
                    }
                    newNode = sdd.disjunction(e.getResult(), newNode.getResult(), handler);
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
