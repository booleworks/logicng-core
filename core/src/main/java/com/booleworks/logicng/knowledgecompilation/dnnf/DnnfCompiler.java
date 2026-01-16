// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.DNNF_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_SHANNON_EXPANSION;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTreeNode;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import com.booleworks.logicng.predicates.satisfiability.SatPredicate;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.transformations.cnf.CnfSubsumption;
import com.booleworks.logicng.transformations.simplification.BackboneSimplifier;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
     * @param maxClauseSize  the maximum clause size of the non-unit clauses
     */
    protected DnnfCompiler(final FormulaFactory f, final Formula originalCnf, final Formula unitClauses, final Formula nonUnitClauses,
                           final int maxClauseSize) {
        this.f = f;
        this.originalCnf = originalCnf;
        this.unitClauses = unitClauses;
        this.nonUnitClauses = nonUnitClauses;
        solver = new DnnfCoreSolver(f, this.originalCnf.variables(f).size());
        solver.add(this.originalCnf);
        numberOfVariables = this.originalCnf.variables(f).size();
        cache = new HashMap<>();
        leafResultOperands = new ArrayList<>(maxClauseSize);
        leafCurrentLiterals = new ArrayList<>(maxClauseSize);
    }

    /**
     * Compiles the given formula to a DNNF instance.
     * @param f       the formula factory
     * @param formula the formula
     * @return the compiled DNNF
     */
    public static Dnnf compile(final FormulaFactory f, final Formula formula) {
        return compile(f, formula, NopHandler.get()).getResult();
    }

    /**
     * Compiles the given formula to a DNNF instance.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param handler the computation handler
     * @return the compiled DNNF
     */
    public static LngResult<Dnnf> compile(final FormulaFactory f, final Formula formula, final ComputationHandler handler) {
        return prepareAndStartComputation(f, formula, handler);
    }

    private static LngResult<Dnnf> prepareAndStartComputation(final FormulaFactory f, final Formula formula, final ComputationHandler handler) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables(f));
        final Formula cnf = formula.cnf(f);
        originalVariables.addAll(cnf.variables(f));
        final LngResult<Formula> simplified = simplifyFormula(f, cnf, handler);
        if (!simplified.isSuccess()) {
            return LngResult.canceled(simplified.getCancelCause());
        }
        final Formula simplifiedFormula = simplified.getResult();

        final Pair<Formula, Formula> unitAndNonUnitClauses = splitCnfClauses(simplifiedFormula, f);
        final Formula unitClauses = unitAndNonUnitClauses.getFirst();
        final Formula nonUnitClauses = unitAndNonUnitClauses.getSecond();
        if (nonUnitClauses.isAtomicFormula()) {
            return LngResult.of(new Dnnf(originalVariables, simplifiedFormula));
        }
        if (!simplifiedFormula.holds(new SatPredicate(f))) {
            return LngResult.of(new Dnnf(originalVariables, f.falsum()));
        }
        final LngResult<DTree> dTreeResult = generateDTree(nonUnitClauses, f, handler);
        if (!dTreeResult.isSuccess()) {
            return LngResult.canceled(dTreeResult.getCancelCause());
        }
        return new DnnfCompiler(f, simplifiedFormula, unitClauses, nonUnitClauses, computeMaxClauseSize(nonUnitClauses))
                .start(dTreeResult.getResult(), originalVariables, handler);
    }

    protected static LngResult<DTree> generateDTree(final Formula nonUnitClauses, final FormulaFactory f, final ComputationHandler handler) {
        return new MinFillDTreeGenerator().generate(f, nonUnitClauses, handler);
    }

    protected static LngResult<Formula> simplifyFormula(final FormulaFactory f, final Formula formula, final ComputationHandler handler) {
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

    protected static int computeMaxClauseSize(final Formula cnf) {
        switch (cnf.getType()) {
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

    protected LngResult<Dnnf> start(final DTree tree,
                                    final SortedSet<Variable> originalVariables,
                                    final ComputationHandler handler) {
        if (!solver.start()) {
            return LngResult.of(new Dnnf(originalVariables, f.falsum()));
        }
        tree.initialize(solver);
        initializeCaches(tree);
        if (!handler.shouldResume(DNNF_COMPUTATION_STARTED)) {
            return LngResult.canceled(DNNF_COMPUTATION_STARTED);
        }
        return cnf2Ddnnf(tree, handler).map(result -> new Dnnf(originalVariables, f.and(unitClauses, result)));
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

    protected LngResult<Formula> cnf2Ddnnf(final DTree tree, final ComputationHandler handler) {
        return cnf2Ddnnf(tree, 0, handler);
    }

    protected LngResult<Formula> cnf2Ddnnf(final DTree tree, final int currentShannons, final ComputationHandler handler) {
        final BitSet separator = tree.dynamicSeparator();
        final Formula implied = newlyImpliedLiterals(tree.getStaticVarSet());

        if (separator.isEmpty()) {
            if (tree instanceof DTreeLeaf) {
                return LngResult.of(f.and(implied, leaf2Ddnnf((DTreeLeaf) tree)));
            } else {
                return conjoin(implied, (DTreeNode) tree, currentShannons, handler);
            }
        } else {
            if (!handler.shouldResume(DNNF_SHANNON_EXPANSION)) {
                return LngResult.canceled(DNNF_SHANNON_EXPANSION);
            }

            final int var = chooseShannonVariable(tree, separator, currentShannons);

            /* Positive branch */
            final Formula positiveDnnf;
            if (solver.decide(var, true)) {
                final LngResult<Formula> recursivePositive = cnf2Ddnnf(tree, currentShannons + 1, handler);
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
                    return LngResult.of(f.falsum());
                }
            }

            /* Negative branch */
            Formula negativeDnnf = f.falsum();
            if (solver.decide(var, false)) {
                final LngResult<Formula> recursiveNegative = cnf2Ddnnf(tree, currentShannons + 1, handler);
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
                    return LngResult.of(f.falsum());
                }
            }

            final Literal lit = solver.litForIdx(var);
            final Formula positiveBranch = f.and(lit, positiveDnnf);
            final Formula negativeBranch = f.and(lit.negate(f), negativeDnnf);
            return LngResult.of(f.and(implied, f.or(positiveBranch, negativeBranch)));
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

    protected LngResult<Formula> conjoin(final Formula implied, final DTreeNode tree, final int currentShannons, final ComputationHandler handler) {
        if (implied == f.falsum()) {
            return LngResult.of(f.falsum());
        }
        final LngResult<Formula> left = cnfAux(tree.left(), currentShannons, handler);
        if (!left.isSuccess() || left.getResult() == f.falsum()) {
            return left;
        }
        final LngResult<Formula> right = cnfAux(tree.right(), currentShannons, handler);
        if (!right.isSuccess() || right.getResult() == f.falsum()) {
            return right;
        }
        return LngResult.of(f.and(implied, left.getResult(), right.getResult()));
    }

    protected LngResult<Formula> cnfAux(final DTree tree, final int currentShannons, final ComputationHandler handler) {
        if (tree instanceof DTreeLeaf) {
            return LngResult.of(leaf2Ddnnf((DTreeLeaf) tree));
        } else {
            final BitSet key = computeCacheKey((DTreeNode) tree, currentShannons);
            if (cache.containsKey(key)) {
                return LngResult.of(cache.get(key));
            } else {
                final LngResult<Formula> dnnf = cnf2Ddnnf(tree, handler);
                if (dnnf.isSuccess() && dnnf.getResult() != f.falsum()) {
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
            switch (solver.valueOf(LngCoreSolver.mkLit(solver.variableIndex(lit), !lit.getPhase()))) {
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
