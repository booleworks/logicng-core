package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * A function computing the cardinality of an SDD, i.e. the minimal/maximal
 * number of variables that must/can be {@code true} in a model.
 * <p>
 * This function can be reused for multiple SDD nodes.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddCardinalityFunction implements SddFunction<Integer> {
    private final Sdd sdd;
    private final Set<Variable> variables;
    private final boolean maximize;

    /**
     * Constructs a new cardinality function.
     * <p>
     * This function can be reused for multiple SDD nodes.
     * @param maximize  If {@code false} the minimal cardinality is computed, if
     *                  {@code false} the maximal cardinality is computed.
     * @param variables the relevant variables
     * @param sdd       the SDD container
     */
    public SddCardinalityFunction(final boolean maximize, final Collection<Variable> variables, final Sdd sdd) {
        this.sdd = sdd;
        this.variables = new HashSet<>(variables);
        this.maximize = maximize;
    }

    @Override
    public LngResult<Integer> execute(final SddNode node, final ComputationHandler handler) {
        final SortedSet<Integer> sddVariables = node.variables();
        final Set<Integer> variableIdxs = SddUtil.varsToIndicesOnlyKnown(variables, sdd, new HashSet<>());
        final int variablesNotInSdd = (int) variables
                .stream()
                .filter(v -> !sdd.knows(v) || !sddVariables.contains(sdd.variableToIndex(v)))
                .count();
        final int cardinality;
        if (node.isFalse()) {
            return LngResult.of(-1);
        } else if (node.isTrue()) {
            cardinality = 0;
        } else {
            cardinality = applyRec(node, variableIdxs, sddVariables, new HashMap<>(), sdd);
        }
        return LngResult.of(maximize ? variablesNotInSdd + cardinality : cardinality);
    }

    private int applyRec(final SddNode node, final Set<Integer> relevantVariables, final Set<Integer> sddVariables,
                         final HashMap<SddNode, Integer> cache, final Sdd sdd) {
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
        final VTreeInternal vTree = sdd.vTreeOf(node).asInternal();
        int cardinality = -1;
        for (final SddElement element : decomp) {
            if (element.getSub().isFalse()) {
                continue;
            }
            final int prime = applyRec(element.getPrime(), relevantVariables, sddVariables, cache, sdd);
            final int sub = applyRec(element.getSub(), relevantVariables, sddVariables, cache, sdd);

            if (maximize) {
                final VTree left = vTree.getLeft();
                final VTree right = vTree.getRight();
                final int primeGap =
                        VTreeUtil.gapVarCount(left, sdd.vTreeOf(element.getPrime()), sdd.getVTree(), sddVariables);
                final int subGap;
                if (element.getSub().isTrue()) {
                    subGap = VTreeUtil.varCount(vTree.getRight(), sddVariables);
                } else {
                    subGap = VTreeUtil.gapVarCount(right, sdd.vTreeOf(element.getSub()), sdd.getVTree(), sddVariables);
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
