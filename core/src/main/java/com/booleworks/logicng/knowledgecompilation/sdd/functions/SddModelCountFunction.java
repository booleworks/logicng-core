package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddQuantification;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SddModelCountFunction implements SddFunction<BigInteger> {
    private final Sdd sdd;
    private final Set<Variable> variables;
    private SortedSet<Integer> sddVariables;

    public SddModelCountFunction(final Collection<Variable> variables, final Sdd sdd) {
        this.sdd = sdd;
        this.variables = new HashSet<>(variables);
    }

    @Override
    public LngResult<BigInteger> execute(final SddNode node, final ComputationHandler handler) {
        final LngResult<SddNode> projectedNodeResult = projectNodeToVariables(node, handler);
        if (!projectedNodeResult.isSuccess()) {
            return LngResult.canceled(projectedNodeResult.getCancelCause());
        }
        final SddNode projectedNode = projectedNodeResult.getResult();

        sddVariables = projectedNode.variables();
        final long dontCareVariables = variables
                .stream()
                .filter(v -> !sdd.knows(v) || !sddVariables.contains(sdd.variableToIndex(v)))
                .count();
        final BigInteger count;
        if (projectedNode.isFalse()) {
            count = BigInteger.ZERO;
        } else if (projectedNode.isTrue()) {
            count = BigInteger.ONE;
        } else {
            count = applyRec(projectedNode, new HashMap<>());
        }
        return LngResult.of(BigInteger.TWO.pow((int) dontCareVariables).multiply(count));
    }

    private LngResult<SddNode> projectNodeToVariables(final SddNode node, final ComputationHandler handler) {
        final Set<Integer> variableIdxs = SddUtil.varsToIndicesOnlyKnown(variables, sdd, new HashSet<>());
        final SortedSet<Integer> originalSddVariables = node.variables();
        final Set<Integer> notProjectedVariables = new TreeSet<>();
        for (final int variable : originalSddVariables) {
            if (!variableIdxs.contains(variable)) {
                notProjectedVariables.add(variable);
            }
        }
        if (notProjectedVariables.isEmpty()) {
            return LngResult.of(node);
        } else {
            return SddQuantification.exists(notProjectedVariables, node, sdd, handler);
        }
    }

    private BigInteger applyRec(final SddNode node, final HashMap<SddNode, BigInteger> cache) {
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
        final VTreeRoot root = sdd.getVTree();
        final SddNodeDecomposition decomp = node.asDecomposition();
        final VTreeInternal vTree = sdd.vTreeOf(node).asInternal();
        BigInteger modelCount = BigInteger.ZERO;
        for (final SddElement element : decomp) {
            final BigInteger prime = applyRec(element.getPrime(), cache);
            final BigInteger sub = applyRec(element.getSub(), cache);

            if (!element.getSub().isFalse()) {
                final VTree left = vTree.getLeft();
                final VTree right = vTree.getRight();
                final BigInteger primeMc =
                        prime.multiply(BigInteger.TWO.pow(
                                VTreeUtil.gapVarCount(left, sdd.vTreeOf(element.getPrime()), root, sddVariables)));
                final BigInteger subMc;
                if (element.getSub().isTrue()) {
                    subMc = BigInteger.TWO.pow(VTreeUtil.varCount(vTree.getRight(), sddVariables));
                } else {
                    subMc = sub.multiply(BigInteger.TWO.pow(
                            VTreeUtil.gapVarCount(right, sdd.vTreeOf(element.getSub()), root, sddVariables)));
                }
                modelCount = modelCount.add(primeMc.multiply(subMc));
            }
        }
        cache.put(node, modelCount);
        return modelCount;
    }

}
