package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddQuantification;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SddProjectionFunction implements SddFunction<SddNode> {
    private final Set<Variable> variables;
    private final Sdd sdd;

    public SddProjectionFunction(final Set<Variable> variables, final Sdd sdd) {
        this.variables = variables;
        this.sdd = sdd;
    }

    @Override
    public LngResult<SddNode> execute(final SddNode node, final ComputationHandler handler) {
        final Set<Integer> variablesOnSdd = node.variables();
        final Set<Integer> variableIdxs = SddUtil.varsToIndicesOnlyKnown(variables, sdd, new HashSet<>());
        final Set<Integer> variablesToEliminate = variablesOnSdd.stream()
                .filter(v -> !variableIdxs.contains(v))
                .collect(Collectors.toSet());
        if (variablesToEliminate.isEmpty()) {
            return LngResult.of(node);
        } else {
            return SddQuantification.exists(variablesToEliminate, node, sdd, handler);
        }
    }
}
