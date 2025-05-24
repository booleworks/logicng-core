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

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

public class SddModelCountFunction implements SddFunction<BigInteger> {
    private final SddNode node;
    private final Set<Variable> variables;
    private SortedSet<Integer> sddVariables;

    public SddModelCountFunction(final Collection<Variable> variables, final SddNode node) {
        this.node = node;
        this.variables = new HashSet<>(variables);
    }

    @Override
    public LngResult<BigInteger> apply(final Sdd sf, final ComputationHandler handler) {
        final Set<Integer> variableIdxs = Util.varsToIndicesOnlyKnown(variables, sf, new HashSet<>());
        sddVariables = node.variables();
        if (!variableIdxs.containsAll(sddVariables)) {
            throw new IllegalArgumentException(
                    "Model Counting variables must be a superset of the variables contained on the SDD");
        }
        final long variablesNotInSdd = variables
                .stream()
                .filter(v -> !sf.knows(v) || !sddVariables.contains(sf.variableToIndex(v)))
                .count();
        final BigInteger count;
        if (node.isFalse()) {
            count = BigInteger.ZERO;
        } else if (node.isTrue()) {
            count = BigInteger.ONE;
        } else {
            count = applyRec(node, sf.getVTree(), new HashMap<>());
        }
        return LngResult.of(BigInteger.TWO.pow((int) variablesNotInSdd).multiply(count));
    }

    private BigInteger applyRec(final SddNode node, final VTreeRoot root, final HashMap<SddNode, BigInteger> cache) {
        if (node.isFalse()) {
            return BigInteger.ZERO;
        }
        if (node.isTrue() || node.isLiteral()) {
            return BigInteger.ONE;
        }
        final BigInteger cached = cache.get(node);
        if (cached != null) {
            return cached;
        }
        final SddNodeDecomposition decomp = node.asDecomposition();
        final VTreeInternal vTree = node.getVTree().asInternal();
        BigInteger modelCount = BigInteger.ZERO;
        for (final SddElement element : decomp.getElements()) {
            final BigInteger prime = applyRec(element.getPrime(), root, cache);
            final BigInteger sub = applyRec(element.getSub(), root, cache);

            if (!element.getSub().isFalse()) {
                final VTree left = vTree.getLeft();
                final VTree right = vTree.getRight();
                final BigInteger primeMc =
                        prime.multiply(BigInteger.TWO.pow(
                                VTreeUtil.gapVarCount(left, element.getPrime().getVTree(), root, sddVariables)));
                final BigInteger subMc;
                if (element.getSub().isTrue()) {
                    subMc = BigInteger.TWO.pow(VTreeUtil.varCount(vTree.getRight(), sddVariables));
                } else {
                    subMc = sub.multiply(BigInteger.TWO.pow(
                            VTreeUtil.gapVarCount(right, element.getSub().getVTree(), root, sddVariables)));
                }
                modelCount = modelCount.add(primeMc.multiply(subMc));
            }
        }
        cache.put(node, modelCount);
        return modelCount;
    }

}
