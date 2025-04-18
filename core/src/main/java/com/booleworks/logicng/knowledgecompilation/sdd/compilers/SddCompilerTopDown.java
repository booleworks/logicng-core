package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SDD_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SDD_SHANNON_EXPANSION;
import static com.booleworks.logicng.knowledgecompilation.dnnf.DnnfCoreSolver.var;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.DecisionVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.predicates.satisfiability.SatPredicate;
import com.booleworks.logicng.transformations.cnf.CnfSubsumption;
import com.booleworks.logicng.transformations.simplification.BackboneSimplifier;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

public class SddCompilerTopDown {
    protected final VTreeRoot root;
    protected final SddFactory sf;
    protected final FormulaFactory f;

    protected final Caches caches; // TODO: Fix and enable caching!
    protected final SddCoreSolver solver;

    protected SddCompilerTopDown(final Formula cnf, final VTreeRoot root, final Caches caches,
                                 final SddCoreSolver solver, final SddFactory sf) {
        this.root = root;
        this.sf = sf;
        this.f = sf.getFactory();
        this.caches = caches;
        this.solver = solver;
        solver.add(cnf);
    }

    public static LngResult<SddCompilationResult> compile(final Formula formula, final SddFactory sf,
                                                          final ComputationHandler handler) {

        return prepareAndStartComputation(formula, sf, handler);
    }

    protected static LngResult<SddCompilationResult> prepareAndStartComputation(final Formula formula,
                                                                                final SddFactory sf,
                                                                                final ComputationHandler handler) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables(sf.getFactory()));
        final Formula cnf = formula.cnf(sf.getFactory());
        originalVariables.addAll(cnf.variables(sf.getFactory()));
        final LngResult<Formula> simplified = simplifyFormula(sf.getFactory(), cnf, handler);
        if (!simplified.isSuccess()) {
            return LngResult.canceled(simplified.getCancelCause());
        }
        final Formula simplifiedFormula = simplified.getResult();

        final Pair<Formula, Formula> unitAndNonUnitClauses = splitCnfClauses(simplifiedFormula, sf.getFactory());
        final Formula unitClauses = unitAndNonUnitClauses.getFirst();
        final Formula nonUnitClauses = unitAndNonUnitClauses.getSecond();

        if (simplifiedFormula.getType() == FType.TRUE) {
            return LngResult.of(new SddCompilationResult(sf.verum(), null));
        }
        if (!simplifiedFormula.holds(new SatPredicate(sf.getFactory()))) {
            return LngResult.of(new SddCompilationResult(sf.falsum(), null));
        }

        final Caches caches = new Caches();
        final SddCoreSolver solver = new SddCoreSolver(sf.getFactory(), originalVariables.size());
        solver.add(simplifiedFormula);
        final LngResult<VTree> vTreeResult = generateVTree(nonUnitClauses, unitClauses, caches, solver, sf, handler);
        if (!vTreeResult.isSuccess()) {
            return LngResult.canceled(vTreeResult.getCancelCause());
        }
        final VTreeRoot root = sf.constructRoot(vTreeResult.getResult());
        final LngResult<SddNode> sdd = new SddCompilerTopDown(cnf, root, caches, solver, sf).start(handler);
        return sdd.map(s -> new SddCompilationResult(s, root));
    }

    protected static LngResult<Formula> simplifyFormula(final FormulaFactory f, final Formula formula,
                                                        final ComputationHandler handler) {
        final LngResult<Formula> backboneSimplified = formula.transform(new BackboneSimplifier(f), handler);
        if (!backboneSimplified.isSuccess()) {
            return LngResult.canceled(backboneSimplified.getCancelCause());
        }
        return backboneSimplified.getResult().transform(new CnfSubsumption(f), handler);
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

    protected static LngResult<VTree> generateVTree(final Formula nonUnitClauses, final Formula unitClauses,
                                                    final Caches caches, final SddCoreSolver solver,
                                                    final SddFactory sf, final ComputationHandler handler) {
        final Set<Variable> nonUnitVars = nonUnitClauses.variables(sf.getFactory());
        final Set<Variable> varsOnlyInUnitClauses = new TreeSet<>();
        for (final Variable v : unitClauses.variables(sf.getFactory())) {
            if (!nonUnitVars.contains(v)) {
                varsOnlyInUnitClauses.add(v);
            }
        }
        VTree vTree = null;
        DTree dTree = null;
        if (!nonUnitVars.isEmpty()) {
            final LngResult<DTree> dTreeResult =
                    new MinFillDTreeGenerator().generate(sf.getFactory(), nonUnitClauses, handler);
            if (!dTreeResult.isSuccess()) {
                return LngResult.canceled(dTreeResult.getCancelCause());
            }
            dTree = dTreeResult.getResult();
            dTree.initialize(solver);
            final LngResult<VTree> dvTree =
                    new DecisionVTreeGenerator(nonUnitClauses, dTree, solver).generate(sf, handler);
            if (!dvTree.isSuccess()) {
                return dvTree;
            }
            vTree = dvTree.getResult();
        }
        if (!varsOnlyInUnitClauses.isEmpty()) {
            final LngResult<VTree> unitTree = new BalancedVTreeGenerator(varsOnlyInUnitClauses).generate(sf, handler);
            if (!unitTree.isSuccess()) {
                return unitTree;
            }
            if (vTree == null) {
                vTree = unitTree.getResult();
            } else {
                vTree = sf.vTreeInternal(unitTree.getResult(), vTree);
            }
        }
        if (vTree != null) {
            generateVarMasks(vTree, caches, solver);
        }
        generateContextMaps(vTree, dTree, caches);
        return LngResult.of(vTree);
    }

    protected static BitSet generateVarMasks(final VTree vTree, final Caches caches, final SddCoreSolver solver) {
        if (vTree.isLeaf()) {
            final VTreeLeaf leaf = vTree.asLeaf();
            final BitSet varMask = new BitSet();
            varMask.set(solver.variableIndex(leaf.getVariable()));
            caches.varMasks.put(leaf, varMask);
            return varMask;
        } else {
            final VTreeInternal internal = vTree.asInternal();
            final BitSet left = (BitSet) generateVarMasks(internal.getLeft(), caches, solver).clone();
            final BitSet right = generateVarMasks(internal.getRight(), caches, solver);
            left.or(right);
            caches.varMasks.put(internal, left);
            return left;
        }
    }

    protected static void generateContextMaps(final VTree vTree, final DTree dTree, final Caches caches) {
        final List<DTreeLeaf> leaves;
        if (dTree instanceof DTreeNode) {
            leaves = ((DTreeNode) dTree).leafs();
        } else {
            leaves = List.of((DTreeLeaf) dTree);
        }
        final Stack<VTree> stack = new Stack<>();
        stack.push(vTree);
        while (!stack.isEmpty()) {
            final VTree parent = stack.pop();
            final BitSet contextClauseMask = new BitSet();
            final BitSet contextVarMask = (BitSet) caches.varMasks.get(parent).clone();
            for (int i = 0; i < leaves.size(); ++i) {
                final BitSet varMask = (BitSet) caches.varMasks.get(parent).clone();
                varMask.and(leaves.get(i).getStaticVarSet());
                if (!varMask.isEmpty() && !varMask.equals(leaves.get(i).getStaticVarSet())) {
                    contextClauseMask.set(i);
                    contextVarMask.or(leaves.get(i).getStaticVarSet());
                }
            }
            caches.contextCMaps.put(parent, contextClauseMask);
            caches.contextVarMasks.put(parent, contextVarMask);
            if (!parent.isLeaf()) {
                stack.push(parent.asInternal().getLeft());
                stack.push(parent.asInternal().getRight());
            }
        }

    }

    protected LngResult<SddNode> start(final ComputationHandler handler) {
        if (!solver.start()) {
            return LngResult.of(sf.falsum());
        }
        if (!handler.shouldResume(SDD_COMPUTATION_STARTED)) {
            return LngResult.canceled(SDD_COMPUTATION_STARTED);
        }
        return cnf2sdd(root.getRoot(), handler);
    }

    protected LngResult<SddNode> cnf2sdd(final VTree tree, final ComputationHandler handler) {
        if (tree.isLeaf()) {
            return cnf2sddLeaf(tree.asLeaf(), handler);
        }
        final VTreeInternal treeInternal = tree.asInternal();
        if (!treeInternal.isShannon()) {
            return cnf2sddDecomp(treeInternal, handler);
        } else {
            return cnf2sddShannon(treeInternal, handler);
        }

    }

    protected LngResult<SddNode> cnf2sddLeaf(final VTreeLeaf leaf, final ComputationHandler handler) {
        final int leafVarIdx = solver.variableIndex(leaf.getVariable());
        final Literal implied = solver.isImplied(leafVarIdx);
        if (implied != null) {
            return LngResult.of(sf.terminal(implied, root));
        } else {
            return LngResult.of(sf.verum());
        }
    }

    protected LngResult<SddNode> cnf2sddDecomp(final VTreeInternal treeInternal, final ComputationHandler handler) {
        final LngResult<SddNode> primeResult = cnf2sdd(treeInternal.getLeft(), handler);
        if (!primeResult.isSuccess()) {
            return primeResult;
        }
        final SddNode prime = primeResult.getResult();
        if (prime.isFalse()) {
            invalidateCache(treeInternal.getLeft());
            return primeResult;
        }
        final LngResult<SddNode> subResult = cnf2sdd(treeInternal.getRight(), handler);
        if (!subResult.isSuccess()) {
            return subResult;
        }
        final SddNode sub = subResult.getResult();
        if (sub.isFalse()) {
            invalidateCache(treeInternal);
            return subResult;
        }
        return LngResult.of(unique(prime, sub, sf.negate(prime, root), sf.falsum()));
    }

    protected LngResult<SddNode> cnf2sddShannon(final VTreeInternal treeInternal,
                                                final ComputationHandler handler) {
        final SddNode cached = lookup(treeInternal);
        if (cached != null) {
            return LngResult.of(cached);
        }
        if (!handler.shouldResume(SDD_SHANNON_EXPANSION)) {
            return LngResult.canceled(SDD_SHANNON_EXPANSION);
        }
        final Variable var = treeInternal.getLeft().asLeaf().getVariable();
        final int varIdx = solver.variableIndex(var);
        final Literal implied = solver.isImplied(varIdx);

        if (implied != null) {
            final SddNode prime = sf.terminal(implied, root);
            final LngResult<SddNode> subResult = cnf2sdd(treeInternal.getRight(), handler);
            if (!subResult.isSuccess()) {
                return subResult;
            }
            final SddNode sub = subResult.getResult();
            if (sub.isFalse()) {
                return subResult;
            }
            return LngResult.of(unique(prime, sub, sf.negate(prime, root), sf.falsum()));
        }
        final SddNode positive;
        if (solver.decide(varIdx, true)) {
            final LngResult<SddNode> positiveResult = cnf2sdd(treeInternal.getRight(), handler);
            if (!positiveResult.isSuccess()) {
                solver.undoDecide(varIdx);
                return positiveResult;
            }
            positive = positiveResult.getResult();
        } else {
            positive = sf.falsum();
        }
        solver.undoDecide(varIdx);
        if (positive.isFalse()) {
            if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                return cnf2sdd(treeInternal, handler);
            } else {
                return LngResult.of(positive);
            }
        }

        final SddNode negative;
        if (solver.decide(varIdx, false)) {
            final LngResult<SddNode> negativeResult = cnf2sdd(treeInternal.getRight(), handler);
            if (!negativeResult.isSuccess()) {
                solver.undoDecide(varIdx);
                return negativeResult;
            }
            negative = negativeResult.getResult();
        } else {
            negative = sf.falsum();
        }
        solver.undoDecide(varIdx);
        if (negative.isFalse()) {
            if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                return cnf2sdd(treeInternal, handler);
            } else {
                return LngResult.of(negative);
            }
        }
        final SddNode lit = sf.terminal(var, root);
        final SddNode litNeg = sf.terminal(var.negate(f), root);
        final SddNode alpha = unique(lit, positive, litNeg, negative);
        addToCache(treeInternal, alpha);
        return LngResult.of(alpha);
    }

    protected void invalidateCache(final VTree vTree) {
        final BitSet key1 = key1(vTree);
        caches.cache.remove(key1);
        if (!vTree.isLeaf()) {
            invalidateCache(vTree.asInternal().getLeft());
            invalidateCache(vTree.asInternal().getRight());
        }
    }

    protected void addToCache(final VTree vTree, final SddNode node) {
        final BitSet key1 = key1(vTree);
        final BitSet key2 = key2(vTree);
        if (!caches.cache.containsKey(key1)) {
            caches.cache.put(key1, new HashMap<>());
        }
        final Map<BitSet, SddNode> level2 = caches.cache.get(key1);
        level2.put(key2, node);
    }

    protected SddNode lookup(final VTree vTree) {
        final BitSet key1 = key1(vTree);
        final Map<BitSet, SddNode> level2 = caches.cache.get(key1);
        if (level2 == null) {
            return null;
        } else {
            final BitSet key2 = key2(vTree);
            return level2.get(key2);
        }
    }

    protected BitSet key1(final VTree vTree) {
        return caches.contextCMaps.get(vTree);
    }

    protected BitSet key2(final VTree vTree) {
        final BitSet contextVarsMap = caches.contextVarMasks.get(vTree);
        //final BitSet varMap = caches.varMasks.get(vTree);
        //contextVarsMap.or(varMap);
        final LngIntVector implied = solver.getImplied();
        final BitSet impliedBitSet = new BitSet();
        for (int i = 0; i < implied.size(); ++i) {
            final int literal = implied.get(i);
            if (contextVarsMap.get(var(literal))) {
                impliedBitSet.set(literal);
            }
        }
        for (int i = contextVarsMap.nextSetBit(0); i != -1; i = contextVarsMap.nextSetBit(i + 1)) {
            if (!impliedBitSet.get(2 * i) && !impliedBitSet.get(2 * i + 1)) {
                impliedBitSet.set(2 * i);
                impliedBitSet.set(2 * i + 1);
            }
        }
        return impliedBitSet;
    }

    protected SddNode unique(final SddNode p1, final SddNode s1, final SddNode p2, final SddNode s2) {
        if (!p1.isFalse() && !p2.isFalse() && s1 == s2) {
            return s1;
        } else if (p1.isFalse() && p2.isTrue()) {
            return s2;
        } else if (p2.isFalse() && p1.isTrue()) {
            return s1;
        } else if (!p1.isFalse() && !p2.isFalse() && s1.isTrue() && s2.isFalse()) {
            return p1;
        } else if (!p1.isFalse() && !p2.isFalse() && s1.isFalse() && s2.isTrue()) {
            return p2;
        }
        final TreeSet<SddElement> elements = new TreeSet<>();
        if (!p1.isFalse()) {
            elements.add(new SddElement(p1, s1));
        }
        if (!p2.isFalse()) {
            elements.add(new SddElement(p2, s2));
        }
        return sf.decomposition(elements, root);
    }

    protected static class Caches {
        protected final Map<BitSet, Map<BitSet, SddNode>> cache = new HashMap<>();
        protected final Map<VTree, BitSet> varMasks = new HashMap<>();
        protected final Map<VTree, BitSet> contextVarMasks = new HashMap<>();
        protected final Map<VTree, BitSet> contextCMaps = new HashMap<>();
    }
}
