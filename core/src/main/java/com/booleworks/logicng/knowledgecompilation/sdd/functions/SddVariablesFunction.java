package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An SDD function computing all variables used in an SDD node and its children.
 * <p>
 * @version 3.0.0
 * @see SddNode#variables(Sdd)
 * @see SddUtil#variables(SddNode)
 * @since 3.0.0
 */
public class SddVariablesFunction implements SddFunction<SortedSet<Variable>> {
    private final Sdd sdd;

    /**
     * Constructs a new function.
     * <p>
     * The function takes an SDD container. Each invocation of this function
     * must be with an SDD node that inhabits the SDD container.
     * @param sdd the SDD container
     */
    public SddVariablesFunction(final Sdd sdd) {
        this.sdd = sdd;
    }

    @Override
    public LngResult<SortedSet<Variable>> execute(final SddNode node, final ComputationHandler handler) {
        final SortedSet<Integer> variableIdxs = node.variables();
        return LngResult.of(SddUtil.indicesToVars(variableIdxs, sdd, new TreeSet<>()));
    }
}
