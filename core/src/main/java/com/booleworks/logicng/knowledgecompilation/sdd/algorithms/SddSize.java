package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * A collection of functions used for calculating the size of an SDD.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddSize {
    private SddSize() {
    }

    /**
     * Computes the size of {@code node}.
     * <p>
     * The size of an SDD is defined as the number of unique nodes it consists
     * of.
     * <p>
     * <i>Note:</i> To compute the size, large parts of the SDD need to be
     * traversed, which can be a major bottleneck if used carelessly.
     * @param node the SDD node
     * @return the size of {@code node}
     */
    public static long size(final SddNode node) {
        return size(List.of(node));
    }

    /**
     * Computes the size of a list of nodes.
     * <p>
     * The size of SDD nodes is defined as the number of unique nodes they
     * consist of.  So nodes appearing in two separate traversals are only
     * counted once.
     * <p>
     * <i>Note:</i> To compute the size, large parts of the SDD need to be
     * traversed, which can be a major bottleneck if used carelessly.
     * @param nodes the SDD nodes
     * @return the size of {@code nodes}
     */
    public static long size(final Collection<SddNode> nodes) {
        final Set<SddNode> visited = new HashSet<>();
        final Stack<SddNode> stack = new Stack<>();
        for (final SddNode node : nodes) {
            if (visited.add(node)) {
                stack.push(node);
            }
        }
        long size = 0;
        while (!stack.isEmpty()) {
            final SddNode current = stack.pop();
            size += 1;
            if (current.isDecomposition()) {
                for (final SddElement element : current.asDecomposition()) {
                    if (visited.add(element.getPrime())) {
                        stack.push(element.getPrime());
                    }
                    if (visited.add(element.getSub())) {
                        stack.push(element.getSub());
                    }
                }
            }
        }
        return size;
    }
}
