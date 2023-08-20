// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.logicng.formulas.cache.TransformationCacheEntry.UNIT_PROPAGATION;

import org.logicng.collections.LNGIntVector;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.solvers.datastructures.MSClause;
import org.logicng.solvers.sat.MiniSat2Solver;
import org.logicng.solvers.sat.MiniSatConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A formula transformation which performs unit propagation.
 * @version 3.0.0
 * @since 1.2
 */
public final class UnitPropagation extends CacheableFormulaTransformation {

    /**
     * Constructs a new transformation.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public UnitPropagation(final FormulaFactory f) {
        super(f, UNIT_PROPAGATION);
    }

    /**
     * Constructs a new transformation.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache for this transformation
     */
    public UnitPropagation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public Formula apply(final Formula formula) {
        final Formula cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final MiniSatPropagator miniSatPropagator = new MiniSatPropagator();
        miniSatPropagator.add(formula);
        final Formula result = miniSatPropagator.propagatedFormula(f);
        setCache(formula, result);
        return result;
    }

    /**
     * An extension of Minisat to propagate units on formulas.
     */
    private class MiniSatPropagator extends MiniSat2Solver {

        /**
         * Constructs a new MiniSatPropagator.
         */
        public MiniSatPropagator() {
            super(MiniSatConfig.builder().incremental(false).build());
        }

        /**
         * Adds an arbitrary formula to the propagator.
         * @param formula the formula
         */
        public void add(final Formula formula) {
            final Formula cnf = formula.cnf(f);
            switch (cnf.type()) {
                case TRUE:
                    break;
                case FALSE:
                case LITERAL:
                case OR:
                    addClause(generateClauseVector(cnf), null);
                    break;
                case AND:
                    for (final Formula op : cnf) {
                        addClause(generateClauseVector(op), null);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected formula type in CNF: " + cnf.type());
            }
        }

        /**
         * Performs unit propagation on level 0 and returns the propagated formula.
         * @param f the formula factory
         * @return the propagated formula
         */
        public Formula propagatedFormula(final FormulaFactory f) {
            assert decisionLevel() == 0;
            if (!ok || propagate() != null) {
                return f.falsum();
            }
            final List<Formula> newClauses = new ArrayList<>();
            for (final MSClause clause : clauses) {
                newClauses.add(clauseToFormula(clause, f));
            }
            for (int i = 0; i < trail.size(); i++) {
                newClauses.add(solverLiteralToFormula(trail.get(i), f));
            }
            return f.and(newClauses);
        }

        /**
         * Transforms a solver literal into the corresponding formula literal.
         * @param lit the solver literal
         * @param f   the formula factory
         * @return the formula literal
         */
        private Literal solverLiteralToFormula(final int lit, final FormulaFactory f) {
            return f.literal(nameForIdx(var(lit)), !sign(lit));
        }

        /**
         * Transforms a solver clause into a formula, respecting the current solver state.
         * I.e. all falsified literals are removed from the resulting clause and
         * if any literal of the clause is satisfied, the result is {@link org.logicng.formulas.CTrue}.
         * @param clause the solver clause to transform
         * @param f      the formula factory
         * @return the transformed clause
         */
        private Formula clauseToFormula(final MSClause clause, final FormulaFactory f) {
            final List<Literal> literals = new ArrayList<>(clause.size());
            for (int i = 0; i < clause.size(); i++) {
                final int lit = clause.get(i);
                switch (value(lit)) {
                    case TRUE:
                        return f.verum();
                    case UNDEF:
                        literals.add(solverLiteralToFormula(lit, f));
                        break;
                    case FALSE:
                        // ignore this literal
                }
            }
            return f.or(literals);
        }

        /**
         * Generates a solver vector of a clause.
         * @param clause the clause
         * @return the solver vector
         */
        private LNGIntVector generateClauseVector(final Formula clause) {
            final LNGIntVector clauseVec = new LNGIntVector(clause.numberOfOperands());
            for (final Literal lit : clause.literals()) {
                int index = idxForName(lit.name());
                if (index == -1) {
                    index = newVar(false, false);
                    addName(lit.name(), index);
                }
                final int litNum = lit.phase() ? index * 2 : (index * 2) ^ 1;
                clauseVec.push(litNum);
            }
            return clauseVec;
        }
    }
}
