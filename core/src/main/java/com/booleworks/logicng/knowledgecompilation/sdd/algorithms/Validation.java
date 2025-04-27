package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.HashSet;

public class Validation {
    private Validation() {
    }

    public static boolean validVTree(final SddNode node, final VTreeRoot root) {
        return validVTree(node, root.getRoot(), root, new HashSet<>());
    }

    private static boolean validVTree(final SddNode node, final VTree expected, final VTreeRoot root,
                                      final HashSet<SddNode> visited) {
        if (!visited.add(node)) {
            return true;
        }
        if (!node.isTrivial() && !checkVTree(node, expected, root)) {
            return false;
        }
        if (node.isDecomposition()) {
            final VTree actual = root.getVTree(node);
            if (actual.isLeaf()) {
                return false;
            }
            for (final SddElement element : node.asDecomposition().getElements()) {
                if (!validVTree(element.getPrime(), actual.asInternal().getLeft(), root, visited)
                        || !validVTree(element.getSub(), actual.asInternal().getRight(), root, visited)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkVTree(final SddNode node, final VTree expected, final VTreeRoot root) {
        final VTree actual = root.getVTree(node);
        if ((node.isTrivial() && actual != null) || (!node.isTrivial() && actual == null)) {
            return false;
        }
        if (!root.isSubtree(actual, expected)) {
            return false;
        }
        return true;
    }
}
