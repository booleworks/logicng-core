package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.SortedSet;
import java.util.TreeSet;

public class SddVariablesFunction implements SddFunction<SortedSet<Variable>> {
    private final SddNode node;

    public SddVariablesFunction(final SddNode node) {
        this.node = node;
    }

    @Override
    public LngResult<SortedSet<Variable>> apply(final SddFactory sf, final ComputationHandler handler) {
        return LngResult.of(applyRec(node, sf));
    }

    private SortedSet<Variable> applyRec(final SddNode currentNode, final SddFactory sf) {
        final SortedSet<Variable> cached = sf.getVariablesCache().get(currentNode);
        if (cached != null) {
            return cached;
        }
        final SortedSet<Variable> variables = new TreeSet<>();
        if (currentNode.isDecomposition()) {
            for (final SddElement element : currentNode.asDecomposition().getElements()) {
                variables.addAll(applyRec(element.getPrime(), sf));
                variables.addAll(applyRec(element.getSub(), sf));
            }
        } else {
            variables.addAll(currentNode.asTerminal().getTerminal().variables(sf.getFactory()));
        }
        sf.getVariablesCache().put(currentNode, variables);
        return variables;
    }
}
