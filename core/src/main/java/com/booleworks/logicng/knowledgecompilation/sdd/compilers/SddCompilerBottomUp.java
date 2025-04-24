package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddApply;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;

import java.util.Collection;
import java.util.List;

public class SddCompilerBottomUp {
    public static LngResult<SddNode> cnfToSdd(final Formula cnf, final VTreeRoot vTree, final SddFactory sf,
                                              final ComputationHandler handler) {
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
                return LngResult.of(sf.verum());
            case FALSE:
                return LngResult.of(sf.falsum());
            default:
                throw new IllegalArgumentException("Expected formula in cnf");
        }
        SddNode node = sf.verum();
        final List<Formula> sorted = Util.sortLitsetsByLca(operands, vTree, sf.getFactory());
        for (final Formula op : sorted) {
            final SddNode l;
            if (op.getType() == FType.LITERAL) {
                l = sf.terminal((Literal) op, vTree);
            } else {
                final LngResult<SddNode> lRes = applyClause(((Or) op).getOperands(), vTree, sf, handler);
                if (!lRes.isSuccess()) {
                    return lRes;
                }
                l = lRes.getResult();
            }
            final LngResult<SddNode> nodeRes =
                    SddApply.apply(l, node, SddApplyOperation.CONJUNCTION, vTree, sf, handler);
            if (!nodeRes.isSuccess()) {
                return nodeRes;
            }
            node = nodeRes.getResult();
        }
        return LngResult.of(node);
    }

    public static LngResult<SddNode> applyClause(final Collection<Formula> lits, final VTreeRoot root,
                                                 final SddFactory sf,
                                                 final ComputationHandler handler) {
        SddNode node = sf.falsum();
        for (final Formula lit : lits) {
            final SddNode l = sf.terminal((Literal) lit, root);
            final LngResult<SddNode> s = SddApply.apply(node, l, SddApplyOperation.DISJUNCTION, root, sf, handler);
            if (!s.isSuccess()) {
                return s;
            }
            node = s.getResult();
        }
        return LngResult.of(node);
    }
}
