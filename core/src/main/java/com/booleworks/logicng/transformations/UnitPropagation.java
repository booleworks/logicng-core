// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.CTrue;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;

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
     * Constructs a new transformation. For a caching formula factory, the cache
     * of the factory will be used, for a non-caching formula factory no cache
     * will be used.
     * @param f the formula factory to generate new formulas
     */
    public UnitPropagation(final FormulaFactory f) {
        super(f, TransformationCacheEntry.UNIT_PROPAGATION);
    }

    /**
     * Constructs a new transformation. For all factory type the provided cache
     * will be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache for this transformation
     */
    public UnitPropagation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        final Formula cached = lookupCache(formula);
        if (cached != null) {
            return LngResult.of(cached);
        }
        final Propagator propagator = new Propagator(f);
        propagator.add(formula);
        final Formula result = propagator.propagatedFormula(f);
        setCache(formula, result);
        return LngResult.of(result);
    }

    /**
     * An extension of the Core Solver to propagate units on formulas.
     */
    private static class Propagator extends LngCoreSolver {

        /**
         * Constructs a new Propagator.
         */
        public Propagator(final FormulaFactory f) {
            super(f, SatSolverConfig.builder().build());
        }

        /**
         * Adds an arbitrary formula to the propagator.
         * @param formula the formula
         */
        public void add(final Formula formula) {
            final Formula cnf = formula.cnf(f);
            switch (cnf.getType()) {
                case TRUE:
                    break;
                case FALSE:
                case LITERAL:
                case OR:
                    addClause(generateClauseVector(cnf.literals(f), this, false, false), null);
                    break;
                case AND:
                    for (final Formula op : cnf) {
                        addClause(generateClauseVector(op.literals(f), this, false, false), null);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected formula type in CNF: " + cnf.getType());
            }
        }

        /**
         * Performs unit propagation on level 0 and returns the propagated
         * formula.
         * @param f the formula factory
         * @return the propagated formula
         */
        public Formula propagatedFormula(final FormulaFactory f) {
            assert decisionLevel() == 0;
            if (!ok || propagate() != null) {
                return f.falsum();
            }
            final List<Formula> newClauses = new ArrayList<>();
            for (final LngClause clause : clauses) {
                newClauses.add(clauseToFormula(f, clause));
            }
            for (int i = 0; i < trail.size(); i++) {
                newClauses.add(solverLiteralToFormula(f, trail.get(i)));
            }
            return f.and(newClauses);
        }

        /**
         * Transforms a solver literal into the corresponding formula literal.
         * @param f   the formula factory
         * @param lit the solver literal
         * @return the formula literal
         */
        private Literal solverLiteralToFormula(final FormulaFactory f, final int lit) {
            return f.literal(nameForIdx(var(lit)), !sign(lit));
        }

        /**
         * Transforms a solver clause into a formula, respecting the current
         * solver state. I.e. all falsified literals are removed from the
         * resulting clause and if any literal of the clause is satisfied, the
         * result is {@link CTrue}.
         * @param f      the formula factory
         * @param clause the solver clause to transform
         * @return the transformed clause
         */
        private Formula clauseToFormula(final FormulaFactory f, final LngClause clause) {
            final List<Literal> literals = new ArrayList<>(clause.size());
            for (int i = 0; i < clause.size(); i++) {
                final int lit = clause.get(i);
                switch (value(lit)) {
                    case TRUE:
                        return f.verum();
                    case UNDEF:
                        literals.add(solverLiteralToFormula(f, lit));
                        break;
                    case FALSE:
                        // ignore this literal
                }
            }
            return f.or(literals);
        }
    }
}
