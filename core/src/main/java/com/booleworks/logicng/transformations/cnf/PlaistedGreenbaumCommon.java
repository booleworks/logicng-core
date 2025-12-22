// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.predicates.ContainsPbcPredicate;
import com.booleworks.logicng.transformations.NnfTransformation;
import com.booleworks.logicng.util.Pair;

import java.util.HashMap;
import java.util.Map;

public abstract class PlaistedGreenbaumCommon<T> {
    protected final FormulaFactory f;
    protected final boolean performNnf;
    protected final Map<Formula, VarCacheEntry> variableCache;
    protected final NnfTransformation nnfTransformation;

    protected PlaistedGreenbaumCommon(final FormulaFactory f, final boolean performNnf) {
        this.f = f;
        this.performNnf = performNnf;
        variableCache = new HashMap<>();
        if (f instanceof CachingFormulaFactory) {
            nnfTransformation = new NnfTransformation(f);
        } else {
            final Map<Formula, Formula> nnfCache = new HashMap<>();
            nnfTransformation = new NnfTransformation(f, nnfCache);
        }
    }

    abstract int newSolverVariable();

    abstract void addToSolver(final LngIntVector clause, T addendum);

    abstract int getLitFromSolver(final Literal lit);

    abstract void addCnf(final Formula cnf, final T addendum);

    private LngIntVector computeTransformation(final Formula formula, final boolean polarity,
                                               final T addendum, final boolean topLevel) {
        switch (formula.getType()) {
            case LITERAL:
                final Literal lit = (Literal) formula;
                return polarity ? vector(getLitFromSolver(lit)) : vector(getLitFromSolver(lit) ^ 1);
            case NOT:
                return computeTransformation(((Not) formula).getOperand(), !polarity, addendum, topLevel);
            case OR:
            case AND:
                return handleNary(formula, polarity, addendum, topLevel);
            case IMPL:
                return handleImplication((Implication) formula, polarity, addendum, topLevel);
            case EQUIV:
                return handleEquivalence((Equivalence) formula, polarity, addendum, topLevel);
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.getType());
        }
    }

    /**
     * Adds the CNF of the given formula (and its optional proposition) to the
     * solver,
     * @param formula  the formula to add to the solver
     * @param addendum the weight of the formula
     */
    public void addCnfToSolver(final Formula formula, final T addendum) {
        final Formula workingFormula = performNnf ? formula.transform(nnfTransformation) : formula;
        final Formula withoutPbcs = !performNnf && workingFormula.holds(ContainsPbcPredicate.get())
                ? workingFormula.nnf(f) : workingFormula;
        if (withoutPbcs.isCnf(f)) {
            addCnf(withoutPbcs, addendum);
        } else {
            final LngIntVector topLevelVars = computeTransformation(withoutPbcs, true, addendum, true);
            if (topLevelVars != null) {
                addToSolver(topLevelVars, addendum);
            }
        }
    }

    /**
     * Clears the cache.
     */
    public void clearCache() {
        variableCache.clear();
    }

    private LngIntVector handleImplication(final Implication formula, final boolean polarity,
                                           final T addendum, final boolean topLevel) {
        final boolean skipPg = polarity || topLevel;
        final Pair<Boolean, Integer> pgVarResult = skipPg ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.getFirst()) {
            return polarity ? vector(pgVarResult.getSecond()) : vector(pgVarResult.getSecond() ^ 1);
        }
        final int pgVar = skipPg ? -1 : pgVarResult.getSecond();
        if (polarity) {
            // pg => (~left | right) ~~> ~pg | ~left | right
            // Speed-Up: Skip pg var
            final LngIntVector leftPgVarNeg = computeTransformation(formula.getLeft(), false, addendum, false);
            final LngIntVector rightPgVarPos = computeTransformation(formula.getRight(), true, addendum, false);
            return vector(leftPgVarNeg, rightPgVarPos);
        } else {
            // (~left | right) => pg
            // ~~> (left & ~right) | pg
            // ~~> (left | pg) & (~right | pg)
            final LngIntVector leftPgVarPos = computeTransformation(formula.getLeft(), true, addendum, topLevel);
            final LngIntVector rightPgVarNeg = computeTransformation(formula.getRight(), false, addendum, topLevel);
            if (topLevel) {
                if (leftPgVarPos != null) {
                    addToSolver(leftPgVarPos, addendum);
                }
                if (rightPgVarNeg != null) {
                    addToSolver(rightPgVarNeg, addendum);
                }
                return null;
            } else {
                addToSolver(vector(pgVar, leftPgVarPos), addendum);
                addToSolver(vector(pgVar, rightPgVarNeg), addendum);
                return vector(pgVar ^ 1);
            }
        }
    }

    private LngIntVector handleEquivalence(final Equivalence formula, final boolean polarity,
                                           final T addendum, final boolean topLevel) {
        final Pair<Boolean, Integer> pgVarResult = topLevel ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.getFirst()) {
            return polarity ? vector(pgVarResult.getSecond()) : vector(pgVarResult.getSecond() ^ 1);
        }
        final int pgVar = topLevel ? -1 : pgVarResult.getSecond();
        final LngIntVector leftPgVarPos = computeTransformation(formula.getLeft(), true, addendum, false);
        final LngIntVector leftPgVarNeg = computeTransformation(formula.getLeft(), false, addendum, false);
        final LngIntVector rightPgVarPos = computeTransformation(formula.getRight(), true, addendum, false);
        final LngIntVector rightPgVarNeg = computeTransformation(formula.getRight(), false, addendum, false);
        if (polarity) {
            // pg => (left => right) & (right => left)
            // ~~> (pg & left => right) & (pg & right => left)
            // ~~> (~pg | ~left | right) & (~pg | ~right | left)
            if (topLevel) {
                addToSolver(vector(leftPgVarNeg, rightPgVarPos), addendum);
                addToSolver(vector(leftPgVarPos, rightPgVarNeg), addendum);
                return null;
            } else {
                addToSolver(vector(pgVar ^ 1, leftPgVarNeg, rightPgVarPos), addendum);
                addToSolver(vector(pgVar ^ 1, leftPgVarPos, rightPgVarNeg), addendum);
            }
        } else {
            // (left => right) & (right => left) => pg
            // ~~> ~(left => right) | ~(right => left) | pg
            // ~~> left & ~right | right & ~left | pg
            // ~~> (left | right | pg) & (~right | ~left | pg)
            if (topLevel) {
                addToSolver(vector(leftPgVarPos, rightPgVarPos), addendum);
                addToSolver(vector(leftPgVarNeg, rightPgVarNeg), addendum);
                return null;
            } else {
                addToSolver(vector(pgVar, leftPgVarPos, rightPgVarPos), addendum);
                addToSolver(vector(pgVar, leftPgVarNeg, rightPgVarNeg), addendum);
            }
        }
        return polarity ? vector(pgVar) : vector(pgVar ^ 1);
    }

    private LngIntVector handleNary(final Formula formula, final boolean polarity, final T addendum,
                                    final boolean topLevel) {
        final boolean skipPg =
                topLevel || formula.getType() == FType.AND && !polarity || formula.getType() == FType.OR && polarity;
        final Pair<Boolean, Integer> pgVarResult = skipPg ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.getFirst()) {
            return polarity ? vector(pgVarResult.getSecond()) : vector(pgVarResult.getSecond() ^ 1);
        }
        final int pgVar = skipPg ? -1 : pgVarResult.getSecond();
        switch (formula.getType()) {
            case AND: {
                if (polarity) {
                    // pg => (v1 & ... & vk) ~~> (~pg | v1) & ... & (~pg | vk)
                    for (final Formula op : formula) {
                        final LngIntVector opPgVars = computeTransformation(op, true, addendum, topLevel);
                        if (topLevel) {
                            if (opPgVars != null) {
                                addToSolver(opPgVars, addendum);
                            }
                        } else {
                            addToSolver(vector(pgVar ^ 1, opPgVars), addendum);
                        }
                    }
                    if (topLevel) {
                        return null;
                    }
                } else {
                    // (v1 & ... & vk) ~~> pg = ~v1 | ... | ~vk | pg
                    // Speed-Up: Skip pg var
                    final LngIntVector singleClause = new LngIntVector();
                    for (final Formula op : formula) {
                        final LngIntVector opPgVars = computeTransformation(op, false, addendum, false);
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
                    final LngIntVector singleClause = new LngIntVector();
                    for (final Formula op : formula) {
                        final LngIntVector opPgVars = computeTransformation(op, true, addendum, false);
                        for (int i = 0; i < opPgVars.size(); i++) {
                            singleClause.push(opPgVars.get(i));
                        }
                    }
                    return singleClause;
                } else {
                    // (v1 | ... | vk) => pg ~~> (~v1 | pg) & ... & (~vk | pg)
                    for (final Formula op : formula) {
                        final LngIntVector opPgVars = computeTransformation(op, false, addendum, topLevel);
                        if (topLevel) {
                            if (opPgVars != null) {
                                addToSolver(opPgVars, addendum);
                            }
                        } else {
                            addToSolver(vector(pgVar, opPgVars), addendum);
                        }
                    }
                    if (topLevel) {
                        return null;
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unexpected type: " + formula.getType());
        }
        return polarity ? vector(pgVar) : vector(pgVar ^ 1);
    }

    protected Pair<Boolean, Integer> getPgVar(final Formula formula, final boolean polarity) {
        final VarCacheEntry entry = variableCache.computeIfAbsent(formula, i -> new VarCacheEntry(newSolverVariable()));
        final boolean wasCached = entry.setPolarityCached(polarity);
        final int pgVar = entry.pgVar;
        return new Pair<>(wasCached, pgVar);
    }

    protected static LngIntVector vector(final int... elts) {
        return LngIntVector.of(elts);
    }

    protected static LngIntVector vector(final LngIntVector a, final LngIntVector b) {
        final LngIntVector result = new LngIntVector(a.size() + b.size());
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        for (int i = 0; i < b.size(); i++) {
            result.unsafePush(b.get(i));
        }
        return result;
    }

    protected static LngIntVector vector(final int elt, final LngIntVector a) {
        final LngIntVector result = new LngIntVector(a.size() + 1);
        result.unsafePush(elt);
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        return result;
    }

    protected static LngIntVector vector(final int elt, final LngIntVector a, final LngIntVector b) {
        final LngIntVector result = new LngIntVector(a.size() + b.size() + 1);
        result.unsafePush(elt);
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        for (int i = 0; i < b.size(); i++) {
            result.unsafePush(b.get(i));
        }
        return result;
    }

    protected static class VarCacheEntry {
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
