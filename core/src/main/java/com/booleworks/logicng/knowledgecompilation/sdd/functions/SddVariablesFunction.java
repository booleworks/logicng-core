package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.SortedSet;
import java.util.TreeSet;

public class SddVariablesFunction implements SddFunction<SortedSet<Variable>> {
    private final Sdd sdd;

    public SddVariablesFunction(final Sdd sdd) {
        this.sdd = sdd;
    }

    @Override
    public LngResult<SortedSet<Variable>> execute(final SddNode node, final ComputationHandler handler) {
        final SortedSet<Integer> variableIdxs = node.variables();
        return LngResult.of(SddUtil.indicesToVars(variableIdxs, sdd, new TreeSet<>()));
    }
}
