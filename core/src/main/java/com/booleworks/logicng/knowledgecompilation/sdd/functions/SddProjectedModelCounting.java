package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddQuantification;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
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
    public LngResult<BigInteger> apply(final SddFactory sf, final ComputationHandler handler) {
        final LngResult<SortedSet<Variable>> originalSddVariablesResult =
                sf.apply(new SddVariablesFunction(node), handler);
        if (!originalSddVariablesResult.isSuccess()) {
            return LngResult.canceled(originalSddVariablesResult.getCancelCause());
        }
        final SortedSet<Variable> originalSddVariables = originalSddVariablesResult.getResult();
        final Set<Variable> notProjectedVariables = new TreeSet<>();
        for (final Variable variable : originalSddVariables) {
            if (!variables.contains(variable)) {
                notProjectedVariables.add(variable);
            }
        }
        final LngResult<SddNode> projectedResult =
                SddQuantification.exists(notProjectedVariables, node, root, sf, handler);
        if (!projectedResult.isSuccess()) {
            return LngResult.canceled(projectedResult.getCancelCause());
        }
        final SddNode projected = projectedResult.getResult();
        final LngResult<SortedSet<Variable>> projectedSddVariablesResult =
                sf.apply(new SddVariablesFunction(projected), handler);
        if (!projectedSddVariablesResult.isSuccess()) {
            return LngResult.canceled(projectedSddVariablesResult.getCancelCause());
        }
        return sf.apply(new SddModelCountFunction(variables, projected, root), handler);
    }
}
