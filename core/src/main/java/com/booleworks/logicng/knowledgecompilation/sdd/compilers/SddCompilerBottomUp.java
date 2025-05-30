package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.Collection;
import java.util.List;

public class SddCompilerBottomUp {
    protected final Formula cnf;
    protected final Sdd sdd;

    public SddCompilerBottomUp(final Formula cnf, final Sdd sdd) {
        this.cnf = cnf;
        this.sdd = sdd;
    }

    public static SddNode cnfToSdd(final Formula cnf, final Sdd sf) {
        return cnfToSdd(cnf, sf, NopHandler.get()).getResult();
    }

    public static LngResult<SddNode> cnfToSdd(final Formula cnf, final Sdd sf,
                                              final ComputationHandler handler) {
        final SddCompilerBottomUp compiler = new SddCompilerBottomUp(cnf, sf);
        return compiler.cnfToSdd(handler);
    }

    public LngResult<SddNode> cnfToSdd(final ComputationHandler handler) {
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
        final List<Formula> sorted = Util.sortLitsetsByLca(operands, sdd);
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
                l = sdd.terminal(sdd.vTreeLeaf(lit.variable()), lit.getPhase());
            } else {
                final LngResult<SddNode> lRes = applyClause(((Or) op).getOperands(), handler);
                if (!lRes.isSuccess()) {
                    sdd.unpin(node);
                    return lRes;
                }
                l = lRes.getResult();
            }
            final LngResult<SddNode> nodeRes =
                    sdd.binaryOperation(l, node, SddApplyOperation.CONJUNCTION, handler);
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

    public LngResult<SddNode> applyClause(final Collection<Formula> lits, final ComputationHandler handler) {
        SddNode node = sdd.falsum();
        for (final Formula formula : lits) {
            final Literal lit = (Literal) formula;
            final SddNode l = sdd.terminal(sdd.vTreeLeaf(lit.variable()), lit.getPhase());
            final LngResult<SddNode> s = sdd.binaryOperation(node, l, SddApplyOperation.DISJUNCTION, handler);
            if (!s.isSuccess()) {
                return s;
            }
            node = s.getResult();
        }
        return LngResult.of(node);
    }
}
