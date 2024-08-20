// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.DNNF_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_SHANNON_EXPANSION;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a DNNF compiler based on ideas by Adnan Darwiche in "New
 * advances in compiling CNF to decomposable negation normal form."
 * @version 3.0.0
 * @since 2.0.0
 */
public class DnnfCompiler {

    protected final FormulaFactory f;

    protected final Formula originalCnf;
    protected final Formula unitClauses;
    protected final Formula nonUnitClauses;
    protected final DnnfSatSolver solver;

    protected final int numberOfVariables;

    protected final Map<BitSet, Formula> cache;

    protected BitSet[][] localCacheKeys;
    protected int[][][] localOccurrences;
    protected final List<Formula> leafResultOperands;
    protected final List<Literal> leafCurrentLiterals;

    /**
     * Constructs a new DNNF compiler for the given formula.
     * @param f              the formula factory to generate new formulas
     * @param originalCnf    the formula to compile
     * @param unitClauses    the unit clauses of the cnf
     * @param nonUnitClauses the non-unit clauses of the cnf
     */
    protected DnnfCompiler(final FormulaFactory f, final Formula originalCnf, final DTree tree, final Formula unitClauses, final Formula nonUnitClauses) {
        this.f = f;
        this.originalCnf = originalCnf;
        this.unitClauses = unitClauses;
        this.nonUnitClauses = nonUnitClauses;
        solver = new DnnfCoreSolver(f, this.originalCnf.variables(f).size());
        solver.add(this.originalCnf);
        numberOfVariables = this.originalCnf.variables(f).size();
        cache = new HashMap<>();
        final int maxClauseSize = computeMaxClauseSize(this.originalCnf);
        leafResultOperands = new ArrayList<>(maxClauseSize);
        leafCurrentLiterals = new ArrayList<>(maxClauseSize);
    }

    protected int computeMaxClauseSize(final Formula cnf) {
        switch (cnf.type()) {
            case OR:
                return cnf.numberOfOperands();
            case AND:
                int max = 1;
                for (final Formula op : cnf) {
                    if (op.numberOfOperands() > max) {
                        max = op.numberOfOperands();
                    }
                }
                return max;
            default:
                return 1;
        }
    }

    protected LNGResult<Formula> start(final DTree tree, final ComputationHandler handler) {
        if (!solver.start()) {
            return LNGResult.of(f.falsum());
        }
        tree.initialize(solver);
        initializeCaches(tree);
        if (!handler.shouldResume(DNNF_COMPUTATION_STARTED)) {
            return LNGResult.canceled(DNNF_COMPUTATION_STARTED);
        }
        return cnf2Ddnnf(tree, handler).map(result -> f.and(unitClauses, result));
    }

    protected void initializeCaches(final DTree dTree) {
        final int depth = dTree.depth() + 1;
        final int sep = dTree.widestSeparator() + 1;
        final int variables = originalCnf.variables(f).size();

        localCacheKeys = new BitSet[depth][sep];
        localOccurrences = new int[depth][sep][variables];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < sep; j++) {
                localCacheKeys[i][j] = new BitSet(dTree.size() + variables);
                Arrays.fill(localOccurrences[i][j], -1);
            }
        }
    }

    protected LNGResult<Formula> cnf2Ddnnf(final DTree tree, final ComputationHandler handler) {
        return cnf2Ddnnf(tree, 0, handler);
    }

    protected LNGResult<Formula> cnf2Ddnnf(final DTree tree, final int currentShannons, final ComputationHandler handler) {
        final BitSet separator = tree.dynamicSeparator();
        final Formula implied = newlyImpliedLiterals(tree.staticVarSet());

        if (separator.isEmpty()) {
            if (tree instanceof DTreeLeaf) {
                return LNGResult.of(f.and(implied, leaf2Ddnnf((DTreeLeaf) tree)));
            } else {
                return conjoin(implied, (DTreeNode) tree, currentShannons, handler);
            }
        } else {
            if (!handler.shouldResume(DNNF_SHANNON_EXPANSION)) {
                return LNGResult.canceled(DNNF_SHANNON_EXPANSION);
            }

            final int var = chooseShannonVariable(tree, separator, currentShannons);

            /* Positive branch */
            final Formula positiveDnnf;
            if (solver.decide(var, true)) {
                final LNGResult<Formula> recursivePositive = cnf2Ddnnf(tree, currentShannons + 1, handler);
                if (!recursivePositive.isSuccess()) {
                    solver.undoDecide(var);
                    return recursivePositive;
                }
                positiveDnnf = recursivePositive.getResult();
            } else {
                positiveDnnf = f.falsum();
            }
            solver.undoDecide(var);
            if (positiveDnnf == f.falsum()) {
                if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                    return cnf2Ddnnf(tree, handler);
                } else {
                    return LNGResult.of(f.falsum());
                }
            }

            /* Negative branch */
            Formula negativeDnnf = f.falsum();
            if (solver.decide(var, false)) {
                final LNGResult<Formula> recursiveNegative = cnf2Ddnnf(tree, currentShannons + 1, handler);
                if (!recursiveNegative.isSuccess()) {
                    solver.undoDecide(var);
                    return recursiveNegative;
                }
                negativeDnnf = recursiveNegative.getResult();
            }
            solver.undoDecide(var);
            if (negativeDnnf == f.falsum()) {
                if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                    return cnf2Ddnnf(tree, handler);
                } else {
                    return LNGResult.of(f.falsum());
                }
            }

            final Literal lit = solver.litForIdx(var);
            final Formula positiveBranch = f.and(lit, positiveDnnf);
            final Formula negativeBranch = f.and(lit.negate(f), negativeDnnf);
            return LNGResult.of(f.and(implied, f.or(positiveBranch, negativeBranch)));
        }
    }

    protected int chooseShannonVariable(final DTree tree, final BitSet separator, final int currentShannons) {
        final int[] occurrences = localOccurrences[tree.depth()][currentShannons];
        for (int i = 0; i < occurrences.length; i++) {
            occurrences[i] = separator.get(i) ? 0 : -1;
        }
        tree.countUnsubsumedOccurrences(occurrences);

        int max = -1;
        int maxVal = -1;
        for (int i = separator.nextSetBit(0); i != -1; i = separator.nextSetBit(i + 1)) {
            final int val = occurrences[i];
            if (val > maxVal) {
                max = i;
                maxVal = val;
            }
        }
        return max;
    }

    protected LNGResult<Formula> conjoin(final Formula implied, final DTreeNode tree, final int currentShannons, final ComputationHandler handler) {
        if (implied == f.falsum()) {
            return LNGResult.of(f.falsum());
        }
        final LNGResult<Formula> left = cnfAux(tree.left(), currentShannons, handler);
        if (left.getResult() == null || left.getResult() == f.falsum()) {
            return left;
        }
        final LNGResult<Formula> right = cnfAux(tree.right(), currentShannons, handler);
        if (right.getResult() == null || right.getResult() == f.falsum()) {
            return right;
        }
        return LNGResult.of(f.and(implied, left.getResult(), right.getResult()));
    }

    protected LNGResult<Formula> cnfAux(final DTree tree, final int currentShannons, final ComputationHandler handler) {
        if (tree instanceof DTreeLeaf) {
            return LNGResult.of(leaf2Ddnnf((DTreeLeaf) tree));
        } else {
            final BitSet key = computeCacheKey((DTreeNode) tree, currentShannons);
            if (cache.containsKey(key)) {
                return LNGResult.of(cache.get(key));
            } else {
                final LNGResult<Formula> dnnf = cnf2Ddnnf(tree, handler);
                if (dnnf.getResult() != null && dnnf.getResult() != f.falsum()) {
                    cache.put((BitSet) key.clone(), dnnf.getResult());
                }
                return dnnf;
            }
        }
    }

    protected BitSet computeCacheKey(final DTreeNode tree, final int currentShannons) {
        final BitSet key = localCacheKeys[tree.depth()][currentShannons];
        key.clear();
        tree.cacheKey(key, numberOfVariables);
        return key;
    }

    protected Formula leaf2Ddnnf(final DTreeLeaf leaf) {
        final Iterator<Literal> literals = leaf.clause().literals(f).iterator();
        leafResultOperands.clear();
        leafCurrentLiterals.clear();
        Literal lit;
        int index = 0;
        while (literals.hasNext()) {
            lit = literals.next();
            switch (solver.valueOf(LNGCoreSolver.mkLit(solver.variableIndex(lit), !lit.phase()))) {
                case TRUE:
                    return f.verum();
                case UNDEF:
                    leafCurrentLiterals.add(lit);
                    leafResultOperands.add(f.and(leafCurrentLiterals));
                    leafCurrentLiterals.set(index, lit.negate(f));
                    index++;
            }
        }
        return f.or(leafResultOperands);
    }

    protected Formula newlyImpliedLiterals(final BitSet knownVariables) {
        return solver.newlyImplied(knownVariables);
    }
}
