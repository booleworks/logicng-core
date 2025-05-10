package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.SortedSet;
import java.util.TreeSet;

public class SddVariablesFunction implements SddFunction<SortedSet<Variable>> {
    private final SddNode node;

    public SddVariablesFunction(final SddNode node) {
        this.node = node;
    }

    @Override
    public LngResult<SortedSet<Variable>> apply(final Sdd sf, final ComputationHandler handler) {
        final SortedSet<Integer> variableIdxs = node.variables();
        return LngResult.of(Util.indicesToVars(variableIdxs, sf, new TreeSet<>()));
    }
}
