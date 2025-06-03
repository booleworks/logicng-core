package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;

import java.util.HashSet;

public class Validation {
    private Validation() {
    }

    public static boolean validVTree(final SddNode node, final Sdd sdd) {
        return validVTree(node, sdd.getVTree().getRoot(), sdd, new HashSet<>());
    }

    private static boolean validVTree(final SddNode node, final VTree expected, final Sdd sdd,
                                      final HashSet<SddNode> visited) {
        if (!visited.add(node)) {
            return true;
        }
        if (!node.isTrivial() && !checkVTree(node, expected, sdd)) {
            return false;
        }
        if (node.isDecomposition()) {
            final VTree actual = sdd.vTreeOf(node);
            if (actual.isLeaf()) {
                return false;
            }
            for (final SddElement element : node.asDecomposition()) {
                if (!validVTree(element.getPrime(), actual.asInternal().getLeft(), sdd, visited)
                        || !validVTree(element.getSub(), actual.asInternal().getRight(), sdd, visited)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkVTree(final SddNode node, final VTree expected, final Sdd sdd) {
        final VTree actual = sdd.vTreeOf(node);
        if ((node.isTrivial() && actual != null) || (!node.isTrivial() && actual == null)) {
            return false;
        }
        if (!sdd.getVTree().isSubtree(actual, expected)) {
            return false;
        }
        return true;
    }
}
