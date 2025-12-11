// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddApply;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class SddCompilerBottomUp {
    protected final Formula cnf;
    protected final Sdd sdd;

    protected SddCompilerBottomUp(final Formula cnf, final Sdd sdd) {
        this.cnf = cnf;
        this.sdd = sdd;
    }

    protected static LngResult<SddNode> compile(final Formula cnf, final Sdd sdd, final ComputationHandler handler) {
        final SddCompilerBottomUp compiler = new SddCompilerBottomUp(cnf, sdd);
        return compiler.cnfToSdd(handler);
    }

    protected LngResult<SddNode> cnfToSdd(final ComputationHandler handler) {
        if (!handler.shouldResume(ComputationStartedEvent.SDD_COMPUTATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.SDD_COMPUTATION_STARTED);
        }

        final List<Formula> operands;
        switch (cnf.getType()) {
            case OR:
            case LITERAL:
                operands = List.of(cnf);
                break;
            case AND:
                operands = ((And) cnf).getOperands();
                break;
            case TRUE:
                return LngResult.of(sdd.verum());
            case FALSE:
                return LngResult.of(sdd.falsum());
            default:
                throw new IllegalArgumentException("Expected formula in cnf");
        }
        SddNode node = sdd.verum();
        int nodeThreshold = 1000;
        final List<Formula> sorted = sortLitsetsByLca(operands, sdd);
        for (final Formula op : sorted) {
            if (sdd.getDecompositionCount() >= nodeThreshold) {
                sdd.pin(node);
                sdd.garbageCollectAll();
                sdd.unpin(node);
                if (sdd.getDecompositionCount() >= (int) (((double) nodeThreshold) * 0.8)) {
                    nodeThreshold = Math.min((int) ((double) sdd.getDecompositionCount() * 1.5), 1_000_000);
                }
            }
            final SddNode l;
            if (op.getType() == FType.LITERAL) {
                final Literal lit = (Literal) op;
                l = sdd.terminal(Objects.requireNonNull(sdd.getVTree().getVTreeLeaf(lit.variable())), lit.getPhase());
            } else {
                final LngResult<SddNode> lRes = applyClause(((Or) op).getOperands(), handler);
                if (!lRes.isSuccess()) {
                    sdd.unpin(node);
                    return lRes;
                }
                l = lRes.getResult();
            }
            final LngResult<SddNode> nodeRes =
                    sdd.binaryOperation(l, node, SddApply.Operation.CONJUNCTION, handler);
            if (!nodeRes.isSuccess()) {
                return nodeRes;
            }
            node = nodeRes.getResult();
        }
        sdd.pin(node);
        sdd.garbageCollectAll();
        sdd.unpin(node);
        return LngResult.of(node);
    }

    protected LngResult<SddNode> applyClause(final Collection<Formula> lits, final ComputationHandler handler) {
        SddNode node = sdd.falsum();
        for (final Formula formula : lits) {
            final Literal lit = (Literal) formula;
            final SddNode l =
                    sdd.terminal(Objects.requireNonNull(sdd.getVTree().getVTreeLeaf(lit.variable())), lit.getPhase());
            final LngResult<SddNode> s = sdd.binaryOperation(node, l, SddApply.Operation.DISJUNCTION, handler);
            if (!s.isSuccess()) {
                return s;
            }
            node = s.getResult();
        }
        return LngResult.of(node);
    }

    private static ArrayList<Formula> sortLitsetsByLca(final Collection<Formula> litsets, final Sdd sdd) {
        final ArrayList<Pair<VTree, Formula>> vTrees = new ArrayList<>(litsets.size());
        for (final Formula litset : litsets) {
            final List<Integer> varIdxs =
                    SddUtil.varsToIndicesExpectKnown(litset.variables(sdd.getFactory()), sdd, new ArrayList<>());
            vTrees.add(new Pair<>(VTreeUtil.lcaFromVariables(varIdxs, sdd), litset));
        }
        vTrees.sort((o1, o2) -> {
            final VTree vTree1 = o1.getFirst();
            final VTree vTree2 = o2.getFirst();
            final VTreeRoot root = sdd.getVTree();
            final int pos1 = o1.getFirst().getPosition();
            final int pos2 = o2.getFirst().getPosition();
            if (o1.getSecond() == o2.getSecond()) {
                return 0;
            }
            if (vTree1 != vTree2 && (root.isSubtree(vTree2, vTree1) || (!root.isSubtree(vTree1, vTree2)
                    && pos1 > pos2))) {
                return 1;
            } else if (vTree1 != vTree2 && (root.isSubtree(vTree1, vTree2) || (!root.isSubtree(vTree2, vTree1)
                    && pos1 < pos2))) {
                return -1;
            } else {
                final Set<Variable> ls1 = o1.getSecond().variables(sdd.getFactory());
                final Set<Variable> ls2 = o2.getSecond().variables(sdd.getFactory());
                if (ls1.size() > ls2.size()) {
                    return 1;
                } else if (ls1.size() < ls2.size()) {
                    return -1;
                } else {
                    final Iterator<Variable> i1 = ls1.iterator();
                    final Iterator<Variable> i2 = ls2.iterator();
                    for (int i = 0; i < ls1.size(); ++i) {
                        final Variable e1 = i1.next();
                        final Variable e2 = i2.next();
                        final int cmp = e1.compareTo(e2);
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                    return 0;
                }
            }
        });
        final ArrayList<Formula> sorted = new ArrayList<>(vTrees.size());
        for (final Pair<VTree, Formula> p : vTrees) {
            sorted.add(p.getSecond());
        }
        return sorted;
    }
}
