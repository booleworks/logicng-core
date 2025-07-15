package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SDD_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SDD_SHANNON_EXPANSION;
import static com.booleworks.logicng.solvers.sat.LngCoreSolver.mkLit;
import static com.booleworks.logicng.solvers.sat.LngCoreSolver.sign;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

class SddCompilerTopDown {
    protected final Sdd sf;
    protected final FormulaFactory f;
    protected final Set<Integer> relevantVars;

    protected final Caches caches;
    protected final SddSatSolver solver;

    protected SddCompilerTopDown(final Set<Integer> relevantVars, final Caches caches, final SddSatSolver solver,
                                 final Sdd sf) {
        this.sf = sf;
        this.f = sf.getFactory();
        this.caches = caches;
        this.solver = solver;
        this.relevantVars = relevantVars;
    }

    protected static LngResult<SddNode> compile(final Set<Variable> variables,
                                                final DTree dTree, final Sdd sdd,
                                                final SddSatSolver solver,
                                                final ComputationHandler handler) {
        final Caches caches = new Caches();
        generateVarMasks(sdd.getVTree().getRoot(), caches);
        generateClauseMasks(sdd.getVTree().getRoot(), dTree, caches);
        sdd.defineVTree(sdd.getVTree().getRoot());
        final Set<Integer> relevantVars = SddUtil.varsToIndicesOnlyKnown(variables, sdd, new HashSet<>());
        return new SddCompilerTopDown(relevantVars, caches, solver, sdd).start(handler);
    }

    protected static BitSet generateVarMasks(final VTree vTree, final Caches caches) {
        if (vTree.isLeaf()) {
            final VTreeLeaf leaf = vTree.asLeaf();
            final int solverIdx = leaf.getVariable();
            final BitSet varMask = new BitSet();
            varMask.set(solverIdx);
            caches.varMasks.put(leaf, varMask);
            return varMask;
        } else {
            final VTreeInternal internal = vTree.asInternal();
            final BitSet left = (BitSet) generateVarMasks(internal.getLeft(), caches).clone();
            final BitSet right = generateVarMasks(internal.getRight(), caches);
            left.or(right);
            caches.varMasks.put(internal, left);
            return left;
        }
    }

    protected static void generateClauseMasks(final VTree vTree, final DTree dTree, final Caches caches) {
        final List<DTreeLeaf> leaves;
        if (dTree == null) {
            leaves = List.of();
        } else if (dTree instanceof DTreeNode) {
            leaves = ((DTreeNode) dTree).leafs();
        } else {
            leaves = List.of((DTreeLeaf) dTree);
        }
        generateClauseLiteralMasks(leaves, caches);
        generateVTreeClauseMasks(vTree, leaves, caches);
    }

    protected static void generateClauseLiteralMasks(final Collection<DTreeLeaf> leaves, final Caches caches) {
        for (final DTreeLeaf leaf : leaves) {
            final BitSet litMask = new BitSet();
            for (final int lit : leaf.literals()) {
                litMask.set(lit);
            }
            caches.clauseLitMasks.put(leaf, litMask);
        }
    }

    protected static void generateVTreeClauseMasks(final VTree vTree, final List<DTreeLeaf> leaves,
                                                   final Caches caches) {
        final Stack<VTree> stack = new Stack<>();
        stack.push(vTree);
        while (!stack.isEmpty()) {
            final VTree parent = stack.pop();
            final BitSet contextVarMask = (BitSet) caches.varMasks.get(parent).clone();
            final BitSet varMask = (BitSet) caches.varMasks.get(parent).clone();
            final BitSet contextClauseMask = new BitSet();
            final BitSet clausePosMask = new BitSet();
            final BitSet clauseNegMask = new BitSet();
            for (int c = 0; c < leaves.size(); c++) {
                final DTreeLeaf leaf = leaves.get(c);
                final BitSet intersection = (BitSet) varMask.clone();
                intersection.and(leaf.getStaticVarSet());
                if (!intersection.isEmpty() && !intersection.equals(leaf.getStaticVarSet())) {
                    contextClauseMask.set(c);
                    contextVarMask.or(leaf.getStaticVarSet());
                }
                if (parent.isLeaf()) {
                    final BitSet litMask = caches.clauseLitMasks.get(leaf);
                    if (litMask.get(mkLit(parent.asLeaf().getVariable(), false))) {
                        clausePosMask.set(c);
                    }
                    if (litMask.get(mkLit(parent.asLeaf().getVariable(), true))) {
                        clauseNegMask.set(c);
                    }
                }
            }

            varMask.and(contextVarMask);
            final BitSet contextLitMask = new BitSet();
            for (int i = varMask.nextSetBit(0); i != -1; i = varMask.nextSetBit(i + 1)) {
                contextLitMask.set(i * 2);
                contextLitMask.set(i * 2 + 1);
            }

            parent.setContextLitsInMask(contextLitMask);
            parent.setContextClauseMask(contextClauseMask);
            if (parent.isLeaf()) {
                parent.asLeaf().setClausePosMask(clausePosMask);
                parent.asLeaf().setClauseNegMask(clauseNegMask);
            }

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
        return cnf2sdd(sf.getVTree().getRoot(), handler);
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
        final int solverVarIdx = leaf.getVariable();
        if (!relevantVars.contains(solverVarIdx)) {
            return LngResult.of(sf.verum());
        }
        final int implied = solver.impliedLiteral(solverVarIdx);
        if (implied != -1) {
            return LngResult.of(sf.terminal(leaf, !sign(implied)));
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
        return LngResult.of(unique(prime, sub, sf.negate(prime), sf.falsum()));
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
        final VTreeLeaf varLeaf = treeInternal.getLeft().asLeaf();
        final int solverVarIdx = varLeaf.getVariable();
        final int implied = solver.impliedLiteral(solverVarIdx);

        if (implied != -1) {
            if (!relevantVars.contains(solverVarIdx)) {
                return cnf2sdd(treeInternal.getRight(), handler);
            }
            final SddNode prime = sf.terminal(varLeaf, !sign(implied));
            final LngResult<SddNode> subResult = cnf2sdd(treeInternal.getRight(), handler);
            if (!subResult.isSuccess()) {
                return subResult;
            }
            final SddNode sub = subResult.getResult();
            if (sub.isFalse()) {
                return subResult;
            }
            return LngResult.of(unique(prime, sub, sf.negate(prime), sf.falsum()));
        }
        final SddNode positive;
        if (solver.decide(solverVarIdx, true)) {
            final LngResult<SddNode> positiveResult = cnf2sdd(treeInternal.getRight(), handler);
            if (!positiveResult.isSuccess()) {
                solver.undoDecide(solverVarIdx);
                return positiveResult;
            }
            positive = positiveResult.getResult();
        } else {
            positive = sf.falsum();
        }
        solver.undoDecide(solverVarIdx);
        if (positive.isFalse()) {
            if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                return cnf2sdd(treeInternal, handler);
            } else {
                return LngResult.of(positive);
            }
        }

        final SddNode negative;
        if (solver.decide(solverVarIdx, false)) {
            final LngResult<SddNode> negativeResult = cnf2sdd(treeInternal.getRight(), handler);
            if (!negativeResult.isSuccess()) {
                solver.undoDecide(solverVarIdx);
                return negativeResult;
            }
            negative = negativeResult.getResult();
        } else {
            negative = sf.falsum();
        }
        solver.undoDecide(solverVarIdx);
        if (negative.isFalse()) {
            if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                return cnf2sdd(treeInternal, handler);
            } else {
                return LngResult.of(negative);
            }
        }
        final SddNode alpha;
        if (relevantVars.contains(varLeaf.getVariable())) {
            final SddNode lit = sf.terminal(varLeaf, true);
            final SddNode litNeg = sf.terminal(varLeaf, false);
            alpha = unique(lit, positive, litNeg, negative);
        } else {
            final LngResult<SddNode> disjunction = sf.disjunction(positive, negative, handler);
            if (!disjunction.isSuccess()) {
                return disjunction;
            }
            alpha = disjunction.getResult();
        }
        addToCache(treeInternal, alpha);
        return LngResult.of(alpha);
    }

    protected void invalidateCache(final VTree vTree) {
        vTree.setCache(null);
        if (!vTree.isLeaf()) {
            invalidateCache(vTree.asInternal().getLeft());
            invalidateCache(vTree.asInternal().getRight());
        }
    }

    protected void addToCache(final VTree vTree, final SddNode node) {
        final BitSet key1 = key1(vTree);
        final BitSet key2 = key2(vTree);
        if (vTree.getCache() == null) {
            vTree.setCache(new HashMap<>());
        }
        final Map<BitSet, Map<BitSet, SddNode>> level2 = vTree.getCache();
        if (!level2.containsKey(key1)) {
            level2.put(key1, new HashMap<>());
        }
        final Map<BitSet, SddNode> level3 = level2.get(key1);
        level3.put(key2, node);
    }

    protected SddNode lookup(final VTree vTree) {
        final Map<BitSet, Map<BitSet, SddNode>> level2 = vTree.getCache();
        if (level2 == null) {
            return null;
        }
        final BitSet key1 = key1(vTree);
        final Map<BitSet, SddNode> level3 = level2.get(key1);
        if (level3 == null) {
            return null;
        } else {
            final BitSet key2 = key2(vTree);
            return level3.get(key2);
        }
    }

    protected BitSet key1(final VTree vTree) {
        final BitSet contextLitsInMask = (BitSet) vTree.getContextLitsInMask().clone();
        contextLitsInMask.and(solver.impliedLiteralBitset());
        return contextLitsInMask;
    }

    protected BitSet key2(final VTree vTree) {
        final BitSet key = (BitSet) vTree.getContextClauseMask().clone();
        key.and(solver.subsumedClauseBitset());
        return key;
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
        final ArrayList<SddElement> elements = new ArrayList<>();
        if (!p1.isFalse()) {
            elements.add(new SddElement(p1, s1));
        }
        if (!p2.isFalse()) {
            elements.add(new SddElement(p2, s2));
        }
        return sf.decompOfCompressedPartition(elements);
    }

    protected static class Caches {
        protected final Map<VTree, BitSet> varMasks = new HashMap<>();
        protected final Map<DTreeLeaf, BitSet> clauseLitMasks = new HashMap<>();
    }
}
