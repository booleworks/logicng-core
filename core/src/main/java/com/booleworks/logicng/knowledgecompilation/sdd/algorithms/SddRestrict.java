package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SddRestrict {
    private SddRestrict() {
    }

    public static LngResult<SddNode> restrict(final int variable, final boolean phase, final SddNode node,
                                              final Sdd sf, final ComputationHandler handler) {
        if (node.isTrivial()) {
            return LngResult.of(node);
        }
        return restrictRec(variable, phase, node, sf, handler, new HashMap<>());
    }

    private static LngResult<SddNode> restrictRec(final int var, final boolean phase, final SddNode node, final Sdd sf,
                                                  final ComputationHandler handler, final Map<SddNode, SddNode> cache) {
        final SddNode cached = cache.get(node);
        if (cached != null) {
            return LngResult.of(cached);
        }

        if (node.isTrivial()) {
            return LngResult.of(node);
        } else if (node.isLiteral()) {
            final SddNodeTerminal t = node.asTerminal();
            if (t.getVTree().getVariable() == var && t.getPhase() == phase) {
                return LngResult.of(sf.verum());
            } else if (t.getVTree().getVariable() == var && t.getPhase() != phase) {
                return LngResult.of(sf.falsum());
            } else {
                return LngResult.of(node);
            }
        } else {
            final VTreeInternal vtree = sf.vTreeOf(node).asInternal();
            final VTreeLeaf leaf = sf.vTreeLeaf(var);
            final SddNode restricted;
            if (sf.getVTree().isSubtree(leaf, vtree.getLeft())) {
                final ArrayList<SddElement> elements = new ArrayList<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final LngResult<SddNode> prime =
                            restrictRec(var, phase, element.getPrime(), sf, handler, cache);
                    if (!prime.isSuccess()) {
                        return prime;
                    }
                    if (!prime.getResult().isFalse()) {
                        elements.add(new SddElement(prime.getResult(), element.getSub()));
                    }
                }
                return sf.decompOfPartition(elements, handler);
            } else if (sf.getVTree().isSubtree(leaf, vtree.getRight())) {
                final ArrayList<SddElement> elements = new ArrayList<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final LngResult<SddNode> sub = restrictRec(var, phase, element.getSub(), sf, handler, cache);
                    if (!sub.isSuccess()) {
                        return sub;
                    }
                    elements.add(new SddElement(element.getPrime(), sub.getResult()));
                }
                return sf.decompOfPartition(elements, handler);
            } else {
                restricted = node;
            }
            cache.put(node, restricted);
            return LngResult.of(restricted);
        }
    }
}
