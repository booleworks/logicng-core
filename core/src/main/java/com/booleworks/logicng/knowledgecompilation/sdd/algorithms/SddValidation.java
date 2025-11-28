package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;

import java.util.HashSet;

/**
 * A collection of function that can be used to verify that an SDD adheres
 * certain properties and invariants.
 * <p>
 * These functions are useful for the development of new SDD constructions and
 * transformations to check whether something broken.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddValidation {
    private SddValidation() {
    }

    /**
     * Checks whether each node is correctly normalized with respect to its
     * VTree.
     * @param node the SDD node to check
     * @param sdd  the SDD container of {@code node}
     * @return whether the SDD node is correctly normalized
     */
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
            final VTree actual = node.getVTree();
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
        final VTree actual = node.getVTree();
        if ((node.isTrivial() && actual != null) || (!node.isTrivial() && actual == null)) {
            return false;
        }
        if (!sdd.getVTree().isSubtree(actual, expected)) {
            return false;
        }
        return true;
    }
}
