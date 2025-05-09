package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class SddRestrict {
    private SddRestrict() {
    }

    public static LngResult<SddNode> restrict(final Literal lit, final SddNode node, final VTreeRoot root,
                                              final SddFactory sf, final ComputationHandler handler) {
        if (root.getLeaf(lit.variable()) == null) {
            return LngResult.of(node);
        }
        if (node.isTrivial()) {
            return LngResult.of(node);
        }
        return restrictRec(lit, node, root, sf, handler, new HashMap<>());
    }

    private static LngResult<SddNode> restrictRec(final Literal lit, final SddNode node, final VTreeRoot root,
                                                  final SddFactory sf, final ComputationHandler handler,
                                                  final Map<SddNode, SddNode> cache) {
        final SddNode cached = cache.get(node);
        if (cached != null) {
            return LngResult.of(cached);
        }

        if (node.isTrivial()) {
            return LngResult.of(node);
        } else if (node.isLiteral()) {
            final Literal l = (Literal) node.asTerminal().getTerminal();
            if (l == lit) {
                return LngResult.of(sf.verum());
            } else if (l.negate(sf.getFactory()) == lit) {
                return LngResult.of(sf.falsum());
            } else {
                return LngResult.of(node);
            }
        } else {
            final VTreeInternal vtree = node.getVTree().asInternal();
            final VTreeLeaf leaf = root.getLeaf(lit.variable());
            final SddNode restricted;
            if (root.isSubtree(leaf, vtree.getLeft())) {
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final LngResult<SddNode> prime = restrictRec(lit, element.getPrime(), root, sf, handler, cache);
                    if (!prime.isSuccess()) {
                        return prime;
                    }
                    if (!prime.getResult().isFalse()) {
                        elements.add(new SddElement(prime.getResult(), element.getSub()));
                    }
                }
                return Util.getNodeOfPartition(elements, root, sf, handler);
            } else if (root.isSubtree(leaf, vtree.getRight())) {
                final TreeSet<SddElement> elements = new TreeSet<>();
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final LngResult<SddNode> sub = restrictRec(lit, element.getSub(), root, sf, handler, cache);
                    if (!sub.isSuccess()) {
                        return sub;
                    }
                    elements.add(new SddElement(element.getPrime(), sub.getResult()));
                }
                return Util.getNodeOfPartition(elements, root, sf, handler);
            } else {
                restricted = node;
            }
            cache.put(node, restricted);
            return LngResult.of(restricted);

        }
    }
}
