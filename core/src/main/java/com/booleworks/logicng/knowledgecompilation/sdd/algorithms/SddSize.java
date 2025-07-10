package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SddSize {

    public static long size(final SddNode node) {
        return size(List.of(node));
    }

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
