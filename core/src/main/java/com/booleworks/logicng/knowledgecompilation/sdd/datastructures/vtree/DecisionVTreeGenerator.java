package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

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
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.PriorityMinFillDTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DecisionVTreeGenerator implements VTreeGenerator {
    private final Formula cnf;
    private final DnnfSatSolver solver;
    private final Set<Variable> projectionVariables;
    private final PrioritizationStrategy strategy;

    private final Formula unitClauses;
    private final Formula nonUnitClauses;


    public DecisionVTreeGenerator(final Formula cnf, final Set<Variable> projectionVariables,
                                  final PrioritizationStrategy strategy, final DnnfSatSolver solver) {
        this.cnf = cnf;
        this.solver = solver;
        this.strategy = strategy;
        this.projectionVariables = projectionVariables;

        final Pair<Formula, Formula> unitAndNonUnitClauses = splitCnfClauses(cnf, solver.f());
        unitClauses = unitAndNonUnitClauses.getFirst();
        nonUnitClauses = unitAndNonUnitClauses.getSecond();
    }

    public DecisionVTreeGenerator(final Formula cnf, final Set<Variable> projectionVariables,
                                  final PrioritizationStrategy strategy) {
        this(cnf, projectionVariables, strategy, initSolver(cnf));
    }

    public DecisionVTreeGenerator(final Formula cnf) {
        this(cnf, cnf.variables(cnf.getFactory()), PrioritizationStrategy.NONE, initSolver(cnf));
    }

    private static DnnfSatSolver initSolver(final Formula cnf) {
        final DnnfSatSolver solver = new DnnfCoreSolver(cnf.getFactory(), cnf.variables(cnf.getFactory()).size());
        solver.add(cnf);
        return solver;
    }

    public static LngResult<Pair<DTree, VTree>> generateDecisionVTree(final Formula cnf,
                                                                      final Set<Variable> projectionVariables,
                                                                      final PrioritizationStrategy strategy,
                                                                      final DnnfSatSolver solver, final Sdd sdd,
                                                                      final ComputationHandler handler) {
        return new DecisionVTreeGenerator(cnf, projectionVariables, strategy, solver).generateIntern(sdd, handler);
    }

    public static Pair<DTree, VTree> generateDecisionVTree(final Formula cnf,
                                                           final Set<Variable> projectionVariables,
                                                           final PrioritizationStrategy strategy,
                                                           final DnnfSatSolver solver, final Sdd sdd) {
        return generateDecisionVTree(cnf, projectionVariables, strategy, solver, sdd, NopHandler.get()).getResult();
    }

    @Override
    public LngResult<VTree> generate(final Sdd sdd, final ComputationHandler handler) {
        return generateIntern(sdd, handler).map(Pair::getSecond);
    }

    private LngResult<Pair<DTree, VTree>> generateIntern(final Sdd sdd, final ComputationHandler handler) {
        if (!handler.shouldResume(ComputationStartedEvent.VTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.VTREE_GENERATION_STARTED);
        }

        final Set<Variable> nonUnitVars = nonUnitClauses.variables(sdd.getFactory());
        final Set<Variable> varsOnlyInUnitClauses = new TreeSet<>();
        for (final Variable v : unitClauses.variables(sdd.getFactory())) {
            if (!nonUnitVars.contains(v)) {
                varsOnlyInUnitClauses.add(v);
            }
        }
        VTree vTree = null;
        DTree dTree = null;
        if (!nonUnitVars.isEmpty()) {
            final List<Set<Variable>> priorityPartition = new ArrayList<>();
            if (strategy == PrioritizationStrategy.NONE) {
                priorityPartition.add(nonUnitVars);
            } else {
                final Set<Variable> eliminatedVariables = nonUnitVars
                        .stream()
                        .filter(v -> !projectionVariables.contains(v))
                        .collect(Collectors.toSet());
                if (strategy == PrioritizationStrategy.VAR_DOWN) {
                    priorityPartition.add(projectionVariables);
                    priorityPartition.add(eliminatedVariables);
                } else if (strategy == PrioritizationStrategy.VAR_UP) {
                    priorityPartition.add(eliminatedVariables);
                    priorityPartition.add(projectionVariables);
                } else {
                    throw new IllegalArgumentException("Unknown prioritization strategy");
                }
            }

            final LngResult<DTree> dTreeResult = new PriorityMinFillDTreeGenerator(priorityPartition)
                    .generate(sdd.getFactory(), nonUnitClauses, handler);
            if (!dTreeResult.isSuccess()) {
                return LngResult.canceled(dTreeResult.getCancelCause());
            }
            dTree = dTreeResult.getResult();
            dTree.initialize(solver);
            final LngResult<VTree> vTreeResult = vTreeFromDTree(dTree, sdd, handler);
            if (!vTreeResult.isSuccess()) {
                return LngResult.canceled(vTreeResult.getCancelCause());
            }
            vTree = vTreeResult.getResult();
        }
        if (!varsOnlyInUnitClauses.isEmpty()) {
            final LngResult<VTree> unitTree = new BalancedVTreeGenerator(varsOnlyInUnitClauses).generate(sdd, handler);
            if (!unitTree.isSuccess()) {
                return LngResult.canceled(unitTree.getCancelCause());
            }
            if (vTree == null) {
                vTree = unitTree.getResult();
            } else {
                vTree = sdd.vTreeInternal(unitTree.getResult(), vTree);
            }
        }
        return LngResult.of(new Pair<>(dTree, vTree));
    }

    private LngResult<VTree> vTreeFromDTree(final DTree dTree, final Sdd sdd, final ComputationHandler handler) {
        if (!handler.shouldResume(SimpleEvent.VTREE_CUTSET_GENERATION)) {
            return LngResult.canceled(SimpleEvent.VTREE_CUTSET_GENERATION);
        }
        final HashMap<DTree, BitSet> cutSets = new HashMap<>();
        cutSets.put(dTree, (BitSet) dTree.getStaticVarSet().clone());
        calculateCutSets(dTree, cutSets, new BitSet());
        return LngResult.of(vTreeFromCutSet(dTree, cutSets, sdd));
    }

    private VTree vTreeFromCutSet(final DTree dTree, final HashMap<DTree, BitSet> cutSets, final Sdd sf) {
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
        final ArrayList<Variable> eliminationVars = new ArrayList<>();
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            final Variable var = solver.litForIdx(i).variable();
            if (!projectionVariables.contains(var)) {
                eliminationVars.add(var);
            } else {
                vars.add(var);
            }
        }
        final VTree vtreeElim = RightLinearVTreeGenerator.generateRightLinear(sf, eliminationVars, subtree);
        return RightLinearVTreeGenerator.generateRightLinear(sf, vars, vtreeElim);
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

    public enum PrioritizationStrategy {
        NONE,
        VAR_DOWN,
        VAR_UP,
    }
}
