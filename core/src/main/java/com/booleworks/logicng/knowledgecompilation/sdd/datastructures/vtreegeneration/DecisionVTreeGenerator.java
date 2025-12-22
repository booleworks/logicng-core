// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfCoreSolver;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfSatSolver;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Computes a decision vtree.
 * <p>
 * A decision vtree is generated for a formula in CNF, using the method
 * described in:
 * Oztok, U., Darwiche, A. (2014). On Compiling CNF into Decision-DNNF.
 * In: O’Sullivan, B. (eds) Principles and Practice of Constraint Programming.
 * CP 2014. Lecture Notes in Computer Science, vol 8656. Springer, Cham.
 * @version 3.0.0
 * @since 3.0.0
 */
public class DecisionVTreeGenerator implements VTreeGenerator {
    protected final FormulaFactory f;
    protected final Formula cnf;
    protected final DnnfSatSolver solver;

    protected final Formula unitClauses;
    protected final Formula nonUnitClauses;

    /**
     * Constructs a new decision vtree generator for the variables of a formula
     * in CNF and a solver.
     * @param cnf    the formula in CNF
     * @param solver the solver
     */
    public DecisionVTreeGenerator(final Formula cnf, final DnnfSatSolver solver) {
        this.f = solver.getFactory();
        this.cnf = cnf;
        this.solver = solver;

        final Pair<Formula, Formula> unitAndNonUnitClauses = splitCnfClauses(cnf, f);
        unitClauses = unitAndNonUnitClauses.getFirst();
        nonUnitClauses = unitAndNonUnitClauses.getSecond();
    }

    /**
     * Constructs a new decision vtree generator for the variables of a formula
     * in CNF.
     * @param cnf the formula in CNF
     */
    public DecisionVTreeGenerator(final Formula cnf) {
        this(cnf, initSolver(cnf));
    }

    protected static DnnfSatSolver initSolver(final Formula cnf) {
        final DnnfSatSolver solver = new DnnfCoreSolver(cnf.getFactory(), cnf.variables(cnf.getFactory()).size());
        solver.add(cnf);
        return solver;
    }

    /**
     * Generates a dtree and a decision vtree for a formula in CNF.
     * <p>
     * Expects that a solver knows the variables of the formula.
     * @param cnf     the formula in CNF
     * @param solver  the solver
     * @param builder the vtree builder
     * @param handler the computation handler
     * @return the dtree and decision vtree or the canceling cause if the
     * computation was aborted by the handler
     */
    public static LngResult<Pair<DTree, VTree>> generateDecisionVTree(final Formula cnf,
                                                                      final DnnfSatSolver solver,
                                                                      final VTreeRoot.Builder builder,
                                                                      final ComputationHandler handler) {
        return new DecisionVTreeGenerator(cnf, solver).generateIntern(builder, handler);
    }

    /**
     * Generates a dtree and a decision vtree for a formula in CNF.
     * <p>
     * Expects that a solver knows the variables of the formula.
     * @param cnf     the formula in CNF
     * @param solver  the solver
     * @param builder the vtree builder
     * @return the dtree and decision vtree
     */
    public static Pair<DTree, VTree> generateDecisionVTree(final Formula cnf,
                                                           final DnnfSatSolver solver,
                                                           final VTreeRoot.Builder builder) {
        return generateDecisionVTree(cnf, solver, builder, NopHandler.get()).getResult();
    }

    @Override
    public LngResult<VTree> generate(final VTreeRoot.Builder builder, final ComputationHandler handler) {
        return generateIntern(builder, handler).map(Pair::getSecond);
    }

    protected LngResult<Pair<DTree, VTree>> generateIntern(final VTreeRoot.Builder builder,
                                                           final ComputationHandler handler) {
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }

        final Set<Variable> nonUnitVars = nonUnitClauses.variables(f);
        final Set<Variable> varsOnlyInUnitClauses = new TreeSet<>();
        for (final Variable v : unitClauses.variables(f)) {
            if (!nonUnitVars.contains(v)) {
                varsOnlyInUnitClauses.add(v);
            }
        }
        VTree vTree = null;
        DTree dTree = null;
        if (!nonUnitVars.isEmpty()) {
            final LngResult<DTree> dTreeResult = new MinFillDTreeGenerator()
                    .generate(f, nonUnitClauses, handler);
            if (!dTreeResult.isSuccess()) {
                return LngResult.canceled(dTreeResult.getCancelCause());
            }
            dTree = dTreeResult.getResult();
            dTree.initialize(solver);
            final LngResult<VTree> vTreeResult = vTreeFromDTree(dTree, builder, handler);
            if (!vTreeResult.isSuccess()) {
                return LngResult.canceled(vTreeResult.getCancelCause());
            }
            vTree = vTreeResult.getResult();
        }
        if (!varsOnlyInUnitClauses.isEmpty()) {
            final LngResult<VTree> unitTree =
                    new BalancedVTreeGenerator(varsOnlyInUnitClauses).generate(builder, handler);
            if (!unitTree.isSuccess()) {
                return LngResult.canceled(unitTree.getCancelCause());
            }
            if (vTree == null) {
                vTree = unitTree.getResult();
            } else {
                vTree = builder.vTreeInternal(unitTree.getResult(), vTree);
            }
        }
        return LngResult.of(new Pair<>(dTree, vTree));
    }

    protected LngResult<VTree> vTreeFromDTree(final DTree dTree, final VTreeRoot.Builder builder,
                                              final ComputationHandler handler) {
        if (!handler.shouldResume(SimpleEvent.VTREE_CUTSET_GENERATION)) {
            return LngResult.canceled(SimpleEvent.VTREE_CUTSET_GENERATION);
        }
        final HashMap<DTree, BitSet> cutSets = new HashMap<>();
        cutSets.put(dTree, (BitSet) dTree.getStaticVarSet().clone());
        calculateCutSets(dTree, cutSets, new BitSet());
        return LngResult.of(vTreeFromCutSet(dTree, cutSets, builder));
    }

    protected VTree vTreeFromCutSet(final DTree dTree, final HashMap<DTree, BitSet> cutSets,
                                    final VTreeRoot.Builder builder) {
        final VTree subtree;
        if (dTree instanceof DTreeNode) {
            final VTree l = vTreeFromCutSet(((DTreeNode) dTree).left(), cutSets, builder);
            final VTree r = vTreeFromCutSet(((DTreeNode) dTree).right(), cutSets, builder);
            if (l == null) {
                subtree = r;
            } else if (r == null) {
                subtree = l;
            } else {
                subtree = builder.vTreeInternal(l, r);
            }
        } else {
            subtree = null;
        }
        final BitSet bits = cutSets.get(dTree);
        final ArrayList<Variable> vars = new ArrayList<>();
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            vars.add(solver.litForIdx(i).variable());
        }
        return RightLinearVTreeGenerator.generateRightLinear(builder, vars, subtree);
    }

    protected void calculateCutSets(final DTree dtree, final HashMap<DTree, BitSet> res, final BitSet mask) {
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

    protected static Pair<Formula, Formula> splitCnfClauses(final Formula originalCnf, final FormulaFactory f) {
        final List<Formula> units = new ArrayList<>();
        final List<Formula> nonUnits = new ArrayList<>();
        switch (originalCnf.getType()) {
            case AND:
                for (final Formula clause : originalCnf) {
                    if (clause.isAtomicFormula()) {
                        units.add(clause);
                    } else {
                        nonUnits.add(clause);
                    }
                }
                break;
            case OR:
                nonUnits.add(originalCnf);
                break;
            default:
                units.add(originalCnf);
        }
        return new Pair<>(f.and(units), f.and(nonUnits));
    }

    public Formula getCnf() {
        return cnf;
    }

    public DnnfSatSolver getSolver() {
        return solver;
    }
}
