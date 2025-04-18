package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfSatSolver;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

public class DecisionVTreeGenerator implements VTreeGenerator {
    private final Formula cnf;
    private final DnnfSatSolver solver;
    private final DTree dTree;

    public DecisionVTreeGenerator(final Formula cnf, final DTree dTree, final DnnfSatSolver solver) {
        this.cnf = cnf;
        this.solver = solver;
        this.dTree = dTree;
    }

    @Override
    public LngResult<VTree> generate(final SddFactory sf, final ComputationHandler handler) {
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }
        final HashMap<DTree, BitSet> cutSets = new HashMap<>();
        cutSets.put(dTree, (BitSet) dTree.getStaticVarSet().clone());
        calculateCutSets(dTree, cutSets, new BitSet());
        final VTree vTree = vTreeFromCutSet(dTree, cutSets, sf);
        return LngResult.of(vTree);
    }

    private VTree vTreeFromCutSet(final DTree dTree, final HashMap<DTree, BitSet> cutSets, final SddFactory sf) {
        final VTree subtree;
        if (dTree instanceof DTreeNode) {
            final VTree l = vTreeFromCutSet(((DTreeNode) dTree).left(), cutSets, sf);
            final VTree r = vTreeFromCutSet(((DTreeNode) dTree).right(), cutSets, sf);
            if (l == null) {
                subtree = r;
            } else if (r == null) {
                subtree = l;
            } else {
                subtree = sf.vTreeInternal(l, r);
            }
        } else {
            subtree = null;
        }
        final BitSet bits = cutSets.get(dTree);
        final ArrayList<Variable> vars = new ArrayList<>();
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            vars.add(solver.litForIdx(i).variable());
        }
        return RightLinearVTreeGenerator.generateRightLinear(sf, vars, subtree);
    }

    private void calculateCutSets(final DTree dtree, final HashMap<DTree, BitSet> res, final BitSet mask) {
        if (dtree instanceof DTreeNode) {
            final DTree left = ((DTreeNode) dtree).left();
            final DTree right = ((DTreeNode) dtree).right();
            final BitSet l = (BitSet) left.getStaticVarSet().clone();
            final BitSet r = (BitSet) right.getStaticVarSet().clone();
            final BitSet p = res.get(dtree);
            final BitSet m = (BitSet) mask.clone();
            p.and(r);
            p.and(l);
            m.or(p);
            l.andNot(m);
            r.andNot(m);
            l.andNot(r);
            r.andNot(l);
            res.put(left, l);
            res.put(right, r);
            calculateCutSets(left, res, m);
            calculateCutSets(right, res, m);
        }
    }

    public Formula getCnf() {
        return cnf;
    }

    public DnnfSatSolver getSolver() {
        return solver;
    }

    public DTree getGenteratedDTree() {
        return dTree;
    }
}
