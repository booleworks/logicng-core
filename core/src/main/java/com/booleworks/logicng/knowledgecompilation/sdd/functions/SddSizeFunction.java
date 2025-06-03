package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SddSizeFunction implements SddFunction<Integer> {
    final Collection<SddNode> nodes;

    public SddSizeFunction(final SddNode node) {
        this.nodes = List.of(node);
    }

    public SddSizeFunction(final Collection<SddNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public LngResult<Integer> apply(final Sdd sf, final ComputationHandler handler) {
        final Set<SddNode> visited = new HashSet<>();
        final Stack<SddNode> stack = new Stack<>();
        for (final SddNode node : nodes) {
            if (visited.add(node)) {
                stack.push(node);
            }
        }
        int size = 0;
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
        return LngResult.of(size);
    }
}
