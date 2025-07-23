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

/**
 * A collection of functions for restricting variables on SDDs.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddRestrict {
    private SddRestrict() {
    }

    /**
     * Conditions an SDD with a single literal.
     * @param variable the internal index of the variable
     * @param phase    the phase of the restriction
     * @param node     the SDD node to restrict
     * @param sdd      the SDD container of {@code node}
     * @param handler  the computation handler
     * @return a (new) SDD node conditioned with {@code variable} and
     * {@code phase}
     */
    public static LngResult<SddNode> restrict(final int variable, final boolean phase, final SddNode node,
                                              final Sdd sdd, final ComputationHandler handler) {
        if (node.isTrivial()) {
            return LngResult.of(node);
        }
        return restrictRec(variable, phase, node, sdd, handler, new HashMap<>());
    }

    private static LngResult<SddNode> restrictRec(final int var, final boolean phase, final SddNode node, final Sdd sdd,
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
                return LngResult.of(sdd.verum());
            } else if (t.getVTree().getVariable() == var && t.getPhase() != phase) {
                return LngResult.of(sdd.falsum());
            } else {
                return LngResult.of(node);
            }
        } else {
            final VTreeInternal vtree = sdd.vTreeOf(node).asInternal();
            final VTreeLeaf leaf = sdd.vTreeLeaf(var);
            final SddNode restricted;
            if (sdd.getVTree().isSubtree(leaf, vtree.getLeft())) {
                final ArrayList<SddElement> elements = new ArrayList<>();
                for (final SddElement element : node.asDecomposition()) {
                    final LngResult<SddNode> prime =
                            restrictRec(var, phase, element.getPrime(), sdd, handler, cache);
                    if (!prime.isSuccess()) {
                        return prime;
                    }
                    if (!prime.getResult().isFalse()) {
                        elements.add(new SddElement(prime.getResult(), element.getSub()));
                    }
                }
                return sdd.decompOfPartition(elements, handler);
            } else if (sdd.getVTree().isSubtree(leaf, vtree.getRight())) {
                final ArrayList<SddElement> elements = new ArrayList<>();
                for (final SddElement element : node.asDecomposition()) {
                    final LngResult<SddNode> sub = restrictRec(var, phase, element.getSub(), sdd, handler, cache);
                    if (!sub.isSuccess()) {
                        return sub;
                    }
                    elements.add(new SddElement(element.getPrime(), sub.getResult()));
                }
                return sdd.decompOfPartition(elements, handler);
            } else {
                restricted = node;
            }
            cache.put(node, restricted);
            return LngResult.of(restricted);
        }
    }
}
