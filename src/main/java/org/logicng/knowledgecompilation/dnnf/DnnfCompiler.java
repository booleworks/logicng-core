// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.dnnf;

import static org.logicng.handlers.Handler.start;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.handlers.DnnfCompilationHandler;
import org.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import org.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeGenerator;
import org.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeLeaf;
import org.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import org.logicng.predicates.satisfiability.SATPredicate;
import org.logicng.solvers.sat.MiniSatStyleSolver;
import org.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of a DNNF compiler based on ideas by Adnan Darwiche in
 * "New advances in compiling CNF to decomposable negation normal form."
 * @version 3.0.0
 * @since 2.0.0
 */
public class DnnfCompiler {

    protected final FormulaFactory f;

    protected final Formula cnf;
    protected final Formula unitClauses;
    protected final Formula nonUnitClauses;
    protected final DnnfSatSolver solver;

    protected final int numberOfVariables;

    protected final Map<BitSet, Formula> cache;
    protected DnnfCompilationHandler handler;

    protected BitSet[][] localCacheKeys;
    protected int[][][] localOccurrences;
    protected final List<Formula> leafResultOperands;
    protected final List<Literal> leafCurrentLiterals;

    /**
     * Constructs a new DNNF compiler for the given formula.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula to compile
     */
    public DnnfCompiler(final FormulaFactory f, final Formula formula) {
        this.f = f;
        cnf = formula;
        final Pair<Formula, Formula> pair = initializeClauses();
        unitClauses = f.and(pair.first());
        nonUnitClauses = f.and(pair.second());
        solver = new DnnfMiniSatStyleSolver(f, cnf.variables(f).size());
        solver.add(cnf);
        numberOfVariables = cnf.variables(f).size();
        cache = new HashMap<>();
        final int maxClauseSize = computeMaxClauseSize(cnf);
        leafResultOperands = new ArrayList<>(maxClauseSize);
        leafCurrentLiterals = new ArrayList<>(maxClauseSize);
    }

    /**
     * Performs the compilation using the given DTree generator.
     * @param generator the DTree generator
     * @return the compiled DNNF
     */
    public Formula compile(final DTreeGenerator generator) {
        return compile(generator, null);
    }

    /**
     * Performs the compilation using the given DTree generator and the compilation handler.
     * @param generator the DTree generator
     * @param handler   the compilation handler
     * @return the compiled DNNF
     */
    public Formula compile(final DTreeGenerator generator, final DnnfCompilationHandler handler) {
        if (!cnf.holds(new SATPredicate(f))) {
            return f.falsum();
        }
        final DTree dTree = generateDTree(f, generator);
        return compile(dTree, handler);
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

    protected Pair<Formula, Formula> initializeClauses() {
        final List<Formula> units = new ArrayList<>();
        final List<Formula> nonUnits = new ArrayList<>();
        switch (cnf.type()) {
            case AND:
                for (final Formula clause : cnf) {
                    if (clause.isAtomicFormula()) {
                        units.add(clause);
                    } else {
                        nonUnits.add(clause);
                    }
                }
                break;
            case OR:
                nonUnits.add(cnf);
                break;
            default:
                units.add(cnf);
        }
        return new Pair<>(f.and(units), f.and(nonUnits));
    }

    protected DTree generateDTree(final FormulaFactory f, final DTreeGenerator generator) {
        if (nonUnitClauses.isAtomicFormula()) {
            return null;
        }
        final DTree tree = generator.generate(f, nonUnitClauses);
        tree.initialize(solver);
        return tree;
    }

    protected Formula compile(final DTree dTree, final DnnfCompilationHandler handler) {
        if (nonUnitClauses.isAtomicFormula()) {
            return cnf;
        }
        if (!solver.start()) {
            return f.falsum();
        }
        initializeCaches(dTree);
        this.handler = handler;
        start(handler);

        Formula result;
        try {
            result = cnf2Ddnnf(dTree);
        } catch (final TimeoutException e) {
            result = null;
        }
        this.handler = null;
        return result == null ? null : f.and(unitClauses, result);
    }

    protected void initializeCaches(final DTree dTree) {
        final int depth = dTree.depth() + 1;
        final int sep = dTree.widestSeparator() + 1;
        final int variables = cnf.variables(f).size();

        localCacheKeys = new BitSet[depth][sep];
        localOccurrences = new int[depth][sep][variables];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < sep; j++) {
                localCacheKeys[i][j] = new BitSet(dTree.size() + variables);
                Arrays.fill(localOccurrences[i][j], -1);
            }
        }
    }

    protected Formula cnf2Ddnnf(final DTree tree) throws TimeoutException {
        return cnf2Ddnnf(tree, 0);
    }

    protected Formula cnf2Ddnnf(final DTree tree, final int currentShannons) throws TimeoutException {
        final BitSet separator = tree.dynamicSeparator();
        final Formula implied = newlyImpliedLiterals(tree.staticVarSet());

        if (separator.isEmpty()) {
            if (tree instanceof DTreeLeaf) {
                return f.and(implied, leaf2Ddnnf((DTreeLeaf) tree));
            } else {
                return conjoin(implied, (DTreeNode) tree, currentShannons);
            }
        } else {
            final int var = chooseShannonVariable(tree, separator, currentShannons);

            if (handler != null && !handler.shannonExpansion()) {
                throw new TimeoutException();
            }

            /* Positive branch */
            Formula positiveDnnf = f.falsum();
            if (solver.decide(var, true)) {
                positiveDnnf = cnf2Ddnnf(tree, currentShannons + 1);
            }
            solver.undoDecide(var);
            if (positiveDnnf == f.falsum()) {
                if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                    return cnf2Ddnnf(tree);
                } else {
                    return f.falsum();
                }
            }

            /* Negative branch */
            Formula negativeDnnf = f.falsum();
            if (solver.decide(var, false)) {
                negativeDnnf = cnf2Ddnnf(tree, currentShannons + 1);
            }
            solver.undoDecide(var);
            if (negativeDnnf == f.falsum()) {
                if (solver.atAssertionLevel() && solver.assertCdLiteral()) {
                    return cnf2Ddnnf(tree);
                } else {
                    return f.falsum();
                }
            }

            final Literal lit = solver.litForIdx(var);
            final Formula positiveBranch = f.and(lit, positiveDnnf);
            final Formula negativeBranch = f.and(lit.negate(f), negativeDnnf);
            return f.and(implied, f.or(positiveBranch, negativeBranch));
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

    protected Formula conjoin(final Formula implied, final DTreeNode tree, final int currentShannons) throws TimeoutException {
        final Formula left;
        final Formula right;
        if (implied == f.falsum() ||
                (left = cnfAux(tree.left(), currentShannons)) == f.falsum() ||
                (right = cnfAux(tree.right(), currentShannons)) == f.falsum()) {
            return f.falsum();
        } else {
            return f.and(implied, left, right);
        }
    }

    protected Formula cnfAux(final DTree tree, final int currentShannons) throws TimeoutException {
        if (tree instanceof DTreeLeaf) {
            return leaf2Ddnnf((DTreeLeaf) tree);
        } else {
            final BitSet key = computeCacheKey((DTreeNode) tree, currentShannons);
            if (cache.containsKey(key)) {
                return cache.get(key);
            } else {
                final Formula dnnf = cnf2Ddnnf(tree);
                if (dnnf != f.falsum()) {
                    cache.put((BitSet) key.clone(), dnnf);
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
            switch (solver.valueOf(MiniSatStyleSolver.mkLit(solver.variableIndex(lit), !lit.phase()))) {
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
