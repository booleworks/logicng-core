package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.Collection;
import java.util.TreeSet;

public class SddCartesianProduct {
    private SddCartesianProduct() {
    }

    public static TreeSet<SddElement> cartesianProduct(final Collection<TreeSet<SddElement>> sets,
                                                       final boolean compress, final VTree vTree, final VTreeRoot root,
                                                       final SddFactory sf) {
        TreeSet<SddElement> res = new TreeSet<>();
        Util.pushNewElement(sf.verum(), sf.falsum(), vTree, root, res);
        for (final TreeSet<SddElement> set : sets) {
            res = cartesianProduct(res, set, compress, vTree, root, sf);
        }
        return res;
    }

    private static TreeSet<SddElement> cartesianProduct(final TreeSet<SddElement> left, final TreeSet<SddElement> right,
                                                        final boolean compress, final VTree vTree, final VTreeRoot root,
                                                        final SddFactory sf) {
        final TreeSet<SddElement> product =
                SddMultiply.multiplyDecompositions(left, right, SddApplyOperation.DISJUNCTION, vTree, root, sf);
        if (compress) {
            return compress(product, root, sf);
        }
        return product;
    }

    private static TreeSet<SddElement> compress(final TreeSet<SddElement> product, final VTreeRoot root,
                                                final SddFactory sf) {
        final TreeSet<SddElement> compressed = new TreeSet<>();
        SddNode prevPrime = null;
        SddNode prevSub = null;
        SddElement prev = null;
        for (final SddElement current : product) {
            if (prevPrime == null) {
                prevPrime = current.getPrime();
                prevSub = current.getSub();
                prev = current;
                continue;
            }
            if (current.getSub() == prevSub) {
                prevPrime = SddApply.apply(current.getPrime(), prevPrime, SddApplyOperation.DISJUNCTION, root, sf);
                prev = null;
            } else {
                if (prev != null) {
                    compressed.add(prev);
                } else {
                    compressed.add(new SddElement(prevPrime, prevSub));
                }
                prevPrime = current.getPrime();
                prevSub = current.getSub();
                prev = current;
            }
        }
        return compressed;
    }
}
