package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

public class SddCardinalityFunction implements SddFunction<Integer> {
    private final SddNode node;
    private final VTreeRoot root;
    private final Set<Variable> variables;
    private SortedSet<Integer> sddVariables;
    private final boolean maximize;

    public SddCardinalityFunction(final boolean maximize, final Collection<Variable> variables, final SddNode node,
                                  final VTreeRoot root) {
        this.node = node;
        this.root = root;
        this.variables = new HashSet<>(variables);
        this.maximize = maximize;
    }

    @Override
    public LngResult<Integer> apply(final Sdd sf, final ComputationHandler handler) {
        sddVariables = node.variables();
        final Set<Integer> variableIdxs = Util.varsToIndicesOnlyKnown(variables, sf, new HashSet<>());
        final int variablesNotInSdd = (int) variables
                .stream()
                .filter(v -> !sf.knows(v) || !sddVariables.contains(sf.variableToIndex(v)))
                .count();
        final int cardinality;
        if (node.isFalse()) {
            return LngResult.of(-1);
        } else if (node.isTrue()) {
            cardinality = 0;
        } else {
            cardinality = applyRec(node, variableIdxs, new HashMap<>());
        }
        return LngResult.of(maximize ? variablesNotInSdd + cardinality : cardinality);
    }

    private int applyRec(final SddNode node, final Set<Integer> relevantVariables,
                         final HashMap<SddNode, Integer> cache) {
        assert !node.isFalse();
        if (node.isTrue()) {
            return 0;
        }
        if (node.isLiteral()) {
            if (relevantVariables.contains(node.asTerminal().getVTree().getVariable())) {
                if (node.asTerminal().getPhase()) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
        final Integer cached = cache.get(node);
        if (cached != null) {
            return cached;
        }
        final SddNodeDecomposition decomp = node.asDecomposition();
        final VTreeInternal vTree = node.getVTree().asInternal();
        int cardinality = -1;
        for (final SddElement element : decomp.getElements()) {
            if (element.getSub().isFalse()) {
                continue;
            }
            final int prime = applyRec(element.getPrime(), relevantVariables, cache);
            final int sub = applyRec(element.getSub(), relevantVariables, cache);

            if (maximize) {
                final VTree left = vTree.getLeft();
                final VTree right = vTree.getRight();
                final int primeGap = VTreeUtil.gapVarCount(left, element.getPrime().getVTree(), root,
                        sddVariables);
                final int subGap;
                if (element.getSub().isTrue()) {
                    subGap = VTreeUtil.varCount(vTree.getRight(), sddVariables);
                } else {
                    subGap = VTreeUtil.gapVarCount(right, element.getSub().getVTree(), root,
                            sddVariables);
                }
                final int c = prime + sub + primeGap + subGap;
                if (cardinality == -1 || c > cardinality) {
                    cardinality = c;
                }
            } else {
                final int c = prime + sub;
                if (cardinality == -1 || c < cardinality) {
                    cardinality = c;
                }
            }
        }
        cache.put(node, cardinality);
        return cardinality;
    }
}
