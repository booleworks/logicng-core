package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddQuantification;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.CompactModel;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SddProjectedModelEnumeration implements SddFunction<List<Model>> {
    private final SddNode originalNode;
    private final VTreeRoot root;
    private final Set<Variable> variables;

    public SddProjectedModelEnumeration(final Collection<Variable> variables, final SddNode originalNode,
                                        final VTreeRoot root) {
        this.originalNode = originalNode;
        this.root = root;
        this.variables = new HashSet<>(variables);
    }

    @Override
    public LngResult<List<Model>> apply(final Sdd sf, final ComputationHandler handler) {
        final LngResult<List<CompactModel>> compactResult = applyNoExpand(sf, handler);
        if (!compactResult.isSuccess()) {
            return LngResult.canceled(compactResult.getCancelCause());
        }
        final List<CompactModel> compact = compactResult.getResult();
        final List<Model> expanded = new ArrayList<>();
        for (final CompactModel model : compact) {
            expanded.addAll(model.expand());
        }
        return LngResult.of(expanded);
    }

    public LngResult<List<CompactModel>> applyNoExpand(final Sdd sf, final ComputationHandler handler) {
        final Set<Integer> variableIdxs = Util.varsToIndicesOnlyKnown(variables, sf, new HashSet<>());
        final SortedSet<Integer> originalSddVariables = originalNode.variables();
        final Set<Integer> notProjectedVariables = new TreeSet<>();
        for (final int variable : originalSddVariables) {
            if (!variableIdxs.contains(variable)) {
                notProjectedVariables.add(variable);
            }
        }
        final LngResult<SddNode> projectedResult =
                SddQuantification.exists(notProjectedVariables, originalNode, root, sf, handler);
        if (!projectedResult.isSuccess()) {
            return LngResult.canceled(projectedResult.getCancelCause());
        }
        final SddNode projectedNode = projectedResult.getResult();
        return new SddModelEnumeration(variables, projectedNode, root).applyNoExpand(sf, handler);
    }
}
