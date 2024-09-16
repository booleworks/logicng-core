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
import com.booleworks.logicng.transformations.NNFTransformation;
import com.booleworks.logicng.util.Pair;

import java.util.HashMap;
import java.util.Map;

public abstract class PlaistedGreenbaumCommon<T> {
    protected final FormulaFactory f;
    protected final boolean performNNF;
    protected final Map<Formula, VarCacheEntry> variableCache;
    protected final NNFTransformation nnfTransformation;

    protected PlaistedGreenbaumCommon(final FormulaFactory f, final boolean performNNF) {
        this.f = f;
        this.performNNF = performNNF;
        variableCache = new HashMap<>();
        if (f instanceof CachingFormulaFactory) {
            nnfTransformation = new NNFTransformation(f);
        } else {
            final Map<Formula, Formula> nnfCache = new HashMap<>();
            nnfTransformation = new NNFTransformation(f, nnfCache);
        }
    }

    abstract int newSolverVariable();

    abstract void addToSolver(final LNGIntVector clause, T addendum);

    abstract int getLitFromSolver(final Literal lit);

    abstract void addCNF(final Formula cnf, final T addendum);

    private LNGIntVector computeTransformation(final Formula formula, final boolean polarity,
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
        final Formula workingFormula = performNNF ? formula.transform(nnfTransformation) : formula;
        final Formula withoutPBCs = !performNNF && workingFormula.holds(ContainsPBCPredicate.get())
                ? workingFormula.nnf(f) : workingFormula;
        if (withoutPBCs.isCNF(f)) {
            addCNF(withoutPBCs, addendum);
        } else {
            final LNGIntVector topLevelVars = computeTransformation(withoutPBCs, true, addendum, true);
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

    private LNGIntVector handleImplication(final Implication formula, final boolean polarity,
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
            final LNGIntVector leftPgVarNeg = computeTransformation(formula.getLeft(), false, addendum, false);
            final LNGIntVector rightPgVarPos = computeTransformation(formula.getRight(), true, addendum, false);
            return vector(leftPgVarNeg, rightPgVarPos);
        } else {
            // (~left | right) => pg
            // ~~> (left & ~right) | pg
            // ~~> (left | pg) & (~right | pg)
            final LNGIntVector leftPgVarPos = computeTransformation(formula.getLeft(), true, addendum, topLevel);
            final LNGIntVector rightPgVarNeg = computeTransformation(formula.getRight(), false, addendum, topLevel);
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

    private LNGIntVector handleEquivalence(final Equivalence formula, final boolean polarity,
                                           final T addendum, final boolean topLevel) {
        final Pair<Boolean, Integer> pgVarResult = topLevel ? new Pair<>(false, null) : getPgVar(formula, polarity);
        if (pgVarResult.getFirst()) {
            return polarity ? vector(pgVarResult.getSecond()) : vector(pgVarResult.getSecond() ^ 1);
        }
        final int pgVar = topLevel ? -1 : pgVarResult.getSecond();
        final LNGIntVector leftPgVarPos = computeTransformation(formula.getLeft(), true, addendum, false);
        final LNGIntVector leftPgVarNeg = computeTransformation(formula.getLeft(), false, addendum, false);
        final LNGIntVector rightPgVarPos = computeTransformation(formula.getRight(), true, addendum, false);
        final LNGIntVector rightPgVarNeg = computeTransformation(formula.getRight(), false, addendum, false);
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

    private LNGIntVector handleNary(final Formula formula, final boolean polarity, final T addendum,
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
                        final LNGIntVector opPgVars = computeTransformation(op, true, addendum, topLevel);
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
                    final LNGIntVector singleClause = new LNGIntVector();
                    for (final Formula op : formula) {
                        final LNGIntVector opPgVars = computeTransformation(op, false, addendum, false);
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
                        final LNGIntVector opPgVars = computeTransformation(op, true, addendum, false);
                        for (int i = 0; i < opPgVars.size(); i++) {
                            singleClause.push(opPgVars.get(i));
                        }
                    }
                    return singleClause;
                } else {
                    // (v1 | ... | vk) => pg ~~> (~v1 | pg) & ... & (~vk | pg)
                    for (final Formula op : formula) {
                        final LNGIntVector opPgVars = computeTransformation(op, false, addendum, topLevel);
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

    protected static LNGIntVector vector(final int... elts) {
        return LNGIntVector.of(elts);
    }

    protected static LNGIntVector vector(final LNGIntVector a, final LNGIntVector b) {
        final LNGIntVector result = new LNGIntVector(a.size() + b.size());
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        for (int i = 0; i < b.size(); i++) {
            result.unsafePush(b.get(i));
        }
        return result;
    }

    protected static LNGIntVector vector(final int elt, final LNGIntVector a) {
        final LNGIntVector result = new LNGIntVector(a.size() + 1);
        result.unsafePush(elt);
        for (int i = 0; i < a.size(); i++) {
            result.unsafePush(a.get(i));
        }
        return result;
    }

    protected static LNGIntVector vector(final int elt, final LNGIntVector a, final LNGIntVector b) {
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
