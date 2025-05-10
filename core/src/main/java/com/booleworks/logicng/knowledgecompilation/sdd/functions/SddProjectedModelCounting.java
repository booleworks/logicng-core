package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddQuantification;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SddProjectedModelCounting implements SddFunction<BigInteger> {
    private final SddNode node;
    private final VTreeRoot root;
    private final Set<Variable> variables;

    public SddProjectedModelCounting(final Collection<Variable> variables, final SddNode node, final VTreeRoot root) {
        this.node = node;
        this.root = root;
        this.variables = new HashSet<>(variables);
    }

    @Override
    public LngResult<BigInteger> apply(final Sdd sf, final ComputationHandler handler) {
        final Set<Integer> variableIdxs = Util.varsToIndicesOnlyKnown(variables, sf, new HashSet<>());
        final SortedSet<Integer> originalSddVariables = sf.variables(node);
        final Set<Integer> notProjectedVariables = new TreeSet<>();
        for (final Integer variable : originalSddVariables) {
            if (!variableIdxs.contains(variable)) {
                notProjectedVariables.add(variable);
            }
        }
        final LngResult<SddNode> projectedResult =
                SddQuantification.exists(notProjectedVariables, node, root, sf, handler);
        if (!projectedResult.isSuccess()) {
            return LngResult.canceled(projectedResult.getCancelCause());
        }
        final SddNode projected = projectedResult.getResult();
        return sf.apply(new SddModelCountFunction(variables, projected, root), handler);
    }
}
