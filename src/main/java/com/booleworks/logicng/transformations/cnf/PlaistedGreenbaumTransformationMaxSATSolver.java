// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.predicates.ContainsPBCPredicate;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.transformations.NNFTransformation;
import com.booleworks.logicng.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * A Plaisted-Greenbaum CNF conversion which is performed directly on the
 * internal SAT solver, not on a formula factory.
 * @version 2.0.0
 * @since 1.6.0
 */
public final class PlaistedGreenbaumTransformationMaxSATSolver {

    private final FormulaFactory f;
    private final boolean performNNF;
    private final Map<Formula, VarCacheEntry> variableCache;
    private final NNFTransformation nnfTransformation;
    private final MaxSAT solver;

    /**
     * Constructs a new transformation for a given SAT solver.
     * @param f          the formula factory to generate new formulas
     * @param performNNF flag whether an NNF transformation should be
     *                   performed on the input formula
     * @param solver     the solver
     */
    public PlaistedGreenbaumTransformationMaxSATSolver(final FormulaFactory f, final boolean performNNF,
                                                       final MaxSAT solver) {
        this.f = f;
        this.performNNF = performNNF;
        variableCache = new HashMap<>();
        if (f instanceof CachingFormulaFactory) {
            nnfTransformation = new NNFTransformation(f);
        } else {
            final Map<Formula, Formula> nnfCache = new HashMap<>();
            nnfTransformation = new NNFTransformation(f, nnfCache);
        }
        this.solver = solver;
    }

    /**
     * Adds the CNF of the given formula (and its optional proposition) to the
     * solver,
     * @param formula the formula to add to the solver
     * @param weight  the weight of the formula
     */
    public void addCnfToSolver(final Formula formula, final int weight) {
        final Formula workingFormula = performNNF ? formula.transform(nnfTransformation) : formula;
        final Formula withoutPBCs = !performNNF && workingFormula.holds(ContainsPBCPredicate.get())
                ? workingFormula.nnf(f) : workingFormula;
        if (withoutPBCs.isCNF(f)) {
            addCNF(withoutPBCs, weight);
        } else {
            final LNGIntVector topLevelVars = computeTransformation(withoutPBCs, true, weight, true);
            if (topLevelVars != null) {
                solver.addClause(topLevelVars, weight);
            }
        }
    }

    /**
     * Clears the cache.
     */
    public void clearCache() {
        variableCache.clear();
    }

    private void addCNF(final Formula cnf, final int weight) {
        switch (cnf.type()) {
            case TRUE:
                break;
            case FALSE:
            case LITERAL:
            case OR:
                solver.addClause(cnf, weight);
                break;
            case AND:
                for (final Formula clause : cnf) {
                    solver.addClause(clause, weight);
                }
                break;
            default:
                throw new IllegalArgumentException("Input formula ist not a valid CNF: " + cnf);
        }
    }

    private LNGIntVector computeTransformation(final Formula formula, final boolean polarity,
                                               final int weight, final boolean topLevel) {
        switch (formula.type()) {
            case LITERAL:
                final Literal lit = (Literal) formula;
                return polarity ? vector(solver.literal(lit)) : vector(solver.literal(lit) ^ 1);
            case NOT:
                return computeTransformation(((Not) formula).operand(), !polarity, weight, topLevel);
            case OR:
            case AND:
                return handleNary(formula, polarity, weight, topLevel);
            case IMPL:
                return handleImplication((Implication) formula, polarity, weight, topLevel);
            case EQUIV:
                return handleEquivalence((Equivalence) formula, polarity, weight, topLevel);
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.type());
        }
    }

    private LNGIntVector handleImplication(final Implication formula, final boolean polarity,
                                           final int weight, final boolean topLevel) {
        final boolean skipPg = polarity || topLevel;
        final Pair<Boolean, Integer> pgVarResult = skipPg ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.first()) {
            return polarity ? vector(pgVarResult.second()) : vector(pgVarResult.second() ^ 1);
        }
        final int pgVar = skipPg ? -1 : pgVarResult.second();
        if (polarity) {
            // pg => (~left | right) ~~> ~pg | ~left | right
            // Speed-Up: Skip pg var
            final LNGIntVector leftPgVarNeg = computeTransformation(formula.left(), false, weight, false);
            final LNGIntVector rightPgVarPos = computeTransformation(formula.right(), true, weight, false);
            return vector(leftPgVarNeg, rightPgVarPos);
        } else {
            // (~left | right) => pg
            // ~~> (left & ~right) | pg
            // ~~> (left | pg) & (~right | pg)
            final LNGIntVector leftPgVarPos = computeTransformation(formula.left(), true, weight, topLevel);
            final LNGIntVector rightPgVarNeg = computeTransformation(formula.right(), false, weight, topLevel);
            if (topLevel) {
                if (leftPgVarPos != null) {
                    solver.addClause(leftPgVarPos, weight);
                }
                if (rightPgVarNeg != null) {
                    solver.addClause(rightPgVarNeg, weight);
                }
                return null;
            } else {
                solver.addClause(vector(pgVar, leftPgVarPos), weight);
                solver.addClause(vector(pgVar, rightPgVarNeg), weight);
                return vector(pgVar ^ 1);
            }
        }
    }

    private LNGIntVector handleEquivalence(final Equivalence formula, final boolean polarity,
                                           final int weight, final boolean topLevel) {
        final Pair<Boolean, Integer> pgVarResult = topLevel ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.first()) {
            return polarity ? vector(pgVarResult.second()) : vector(pgVarResult.second() ^ 1);
        }
        final int pgVar = topLevel ? -1 : pgVarResult.second();
        final LNGIntVector leftPgVarPos = computeTransformation(formula.left(), true, weight, false);
        final LNGIntVector leftPgVarNeg = computeTransformation(formula.left(), false, weight, false);
        final LNGIntVector rightPgVarPos = computeTransformation(formula.right(), true, weight, false);
        final LNGIntVector rightPgVarNeg = computeTransformation(formula.right(), false, weight, false);
        if (polarity) {
            // pg => (left => right) & (right => left)
            // ~~> (pg & left => right) & (pg & right => left)
            // ~~> (~pg | ~left | right) & (~pg | ~right | left)
            if (topLevel) {
                solver.addClause(vector(leftPgVarNeg, rightPgVarPos), weight);
                solver.addClause(vector(leftPgVarPos, rightPgVarNeg), weight);
                return null;
            } else {
                solver.addClause(vector(pgVar ^ 1, leftPgVarNeg, rightPgVarPos), weight);
                solver.addClause(vector(pgVar ^ 1, leftPgVarPos, rightPgVarNeg), weight);
            }
        } else {
            // (left => right) & (right => left) => pg
            // ~~> ~(left => right) | ~(right => left) | pg
            // ~~> left & ~right | right & ~left | pg
            // ~~> (left | right | pg) & (~right | ~left | pg)
            if (topLevel) {
                solver.addClause(vector(leftPgVarPos, rightPgVarPos), weight);
                solver.addClause(vector(leftPgVarNeg, rightPgVarNeg), weight);
                return null;
            } else {
                solver.addClause(vector(pgVar, leftPgVarPos, rightPgVarPos), weight);
                solver.addClause(vector(pgVar, leftPgVarNeg, rightPgVarNeg), weight);
            }
        }
        return polarity ? vector(pgVar) : vector(pgVar ^ 1);
    }

    private LNGIntVector handleNary(final Formula formula, final boolean polarity, final int weight,
                                    final boolean topLevel) {
        final boolean skipPg =
                topLevel || formula.type() == FType.AND && !polarity || formula.type() == FType.OR && polarity;
        final Pair<Boolean, Integer> pgVarResult = skipPg ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.first()) {
            return polarity ? vector(pgVarResult.second()) : vector(pgVarResult.second() ^ 1);
        }
        final int pgVar = skipPg ? -1 : pgVarResult.second();
        switch (formula.type()) {
            case AND: {
                if (polarity) {
                    // pg => (v1 & ... & vk) ~~> (~pg | v1) & ... & (~pg | vk)
                    for (final Formula op : formula) {
                        final LNGIntVector opPgVars = computeTransformation(op, true, weight, topLevel);
                        if (topLevel) {
                            if (opPgVars != null) {
                                solver.addClause(opPgVars, weight);
                            }
                        } else {
                            solver.addClause(vector(pgVar ^ 1, opPgVars), weight);
                        }
                    }
                    if (topLevel) {
                        return null;
                    }
                } else {
                    // (v1 & ... & vk) ~~> pg = ~v1 | ... | ~vk | pg
                    // Speed-Up: Skip pg var
                    final LNGIntVector singleClause = new LNGIntVector();
                    for (final Formula op : formula) {
                        final LNGIntVector opPgVars = computeTransformation(op, false, weight, false);
                        for (int i = 0; i < opPgVars.size(); i++) {
                            singleClause.push(opPgVars.get(i));
                        }
                    }
                    return singleClause;
                }
                break;
            }
            case OR: {
                if (polarity) {
                    // pg => (v1 | ... | vk) ~~> ~pg | v1 | ... | vk
                    // Speed-Up: Skip pg var
                    final LNGIntVector singleClause = new LNGIntVector();
                    for (final Formula op : formula) {
                        final LNGIntVector opPgVars = computeTransformation(op, true, weight, false);
                        for (int i = 0; i < opPgVars.size(); i++) {
                            singleClause.push(opPgVars.get(i));
                        }
                    }
                    return singleClause;
                } else {
                    // (v1 | ... | vk) => pg ~~> (~v1 | pg) & ... & (~vk | pg)
                    for (final Formula op : formula) {
                        final LNGIntVector opPgVars = computeTransformation(op, false, weight, topLevel);
                        if (topLevel) {
                            if (opPgVars != null) {
                                solver.addClause(opPgVars, weight);
                            }
                        } else {
                            solver.addClause(vector(pgVar, opPgVars), weight);
                        }
                    }
                    if (topLevel) {
                        return null;
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unexpected type: " + formula.type());
        }
        return polarity ? vector(pgVar) : vector(pgVar ^ 1);
    }

    private Pair<Boolean, Integer> getPgVar(final Formula formula, final boolean polarity) {
        final VarCacheEntry entry = variableCache.computeIfAbsent(formula, i -> new VarCacheEntry(newSolverVariable()));
        final boolean wasCached = entry.setPolarityCached(polarity);
        final int pgVar = entry.pgVar;
        return new Pair<>(wasCached, pgVar);
    }

    private int newSolverVariable() {
        return solver.newVar() * 2;
    }

    private static LNGIntVector vector(final int... elts) {
        return LNGIntVector.of(elts);
    }

    private static LNGIntVector vector(final LNGIntVector a, final LNGIntVector b) {
        final LNGIntVector result = new LNGIntVector(a.size() + b.size());
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        for (int i = 0; i < b.size(); i++) {
            result.unsafePush(b.get(i));
        }
        return result;
    }

    private static LNGIntVector vector(final int elt, final LNGIntVector a) {
        final LNGIntVector result = new LNGIntVector(a.size() + 1);
        result.unsafePush(elt);
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        return result;
    }

    private static LNGIntVector vector(final int elt, final LNGIntVector a, final LNGIntVector b) {
        final LNGIntVector result = new LNGIntVector(a.size() + b.size() + 1);
        result.unsafePush(elt);
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        for (int i = 0; i < b.size(); i++) {
            result.unsafePush(b.get(i));
        }
        return result;
    }

    private static class VarCacheEntry {
        private final Integer pgVar;
        private boolean posPolarityCached = false;
        private boolean negPolarityCached = false;

        public VarCacheEntry(final Integer pgVar) {
            this.pgVar = pgVar;
        }

        public boolean setPolarityCached(final boolean polarity) {
            final boolean wasCached;
            if (polarity) {
                wasCached = posPolarityCached;
                posPolarityCached = true;
            } else {
                wasCached = negPolarityCached;
                negPolarityCached = true;
            }
            return wasCached;
        }
    }
}
