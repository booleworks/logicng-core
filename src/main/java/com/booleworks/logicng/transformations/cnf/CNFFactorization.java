// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.FACTORIZATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DISTRIBUTION_PERFORMED;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.FactorizationCreatedClauseEvent;
import com.booleworks.logicng.transformations.CacheableAndAbortableFormulaTransformation;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Transformation of a formula in CNF by factorization.
 * @version 3.0.0
 * @since 1.0
 */
public final class CNFFactorization extends CacheableAndAbortableFormulaTransformation<ComputationHandler> {

    private boolean proceed = true;

    /**
     * Constructor for a CNF Factorization.
     * @param f the formula factory to generate new formulas
     */
    public CNFFactorization(final FormulaFactory f) {
        super(f, TransformationCacheEntry.FACTORIZED_CNF, NopHandler.get());
    }

    /**
     * Constructor for a CNF Factorization.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public CNFFactorization(final FormulaFactory f, final Map<Formula, Formula> cache) {
        this(f, NopHandler.get(), cache);
    }

    /**
     * Constructor for a CNF Factorization.
     * @param f       the formula factory to generate new formulas
     * @param handler the handler for the transformation
     */
    public CNFFactorization(final FormulaFactory f, final ComputationHandler handler) {
        super(f, TransformationCacheEntry.FACTORIZED_CNF, handler);
    }

    /**
     * Constructs a new transformation. For all factory type the provided cache
     * will be used. If it is null, no cache will be used. The handler - if not
     * null - is used for aborting the computation.
     * @param f       the formula factory to generate new formulas
     * @param cache   the cache to use for the transformation
     * @param handler the handler for the transformation
     */
    public CNFFactorization(final FormulaFactory f, final ComputationHandler handler,
                            final Map<Formula, Formula> cache) {
        super(f, cache, handler);
    }

    @Override
    public Formula apply(final Formula formula) {
        proceed = handler.shouldResume(FACTORIZATION_STARTED);
        return applyRec(formula);
    }

    private Formula applyRec(final Formula formula) {
        if (!proceed) {
            return null;
        }
        if (formula.type().precedence() >= FType.LITERAL.precedence()) {
            return formula;
        }
        Formula cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        switch (formula.type()) {
            case NOT:
            case IMPL:
            case EQUIV:
                cached = applyRec(formula.nnf(f));
                break;
            case OR:
                LinkedHashSet<Formula> nops = new LinkedHashSet<>();
                for (final Formula op : formula) {
                    if (!proceed) {
                        return null;
                    }
                    nops.add(applyRec(op));
                }
                final Iterator<Formula> it = nops.iterator();
                cached = it.next();
                while (it.hasNext()) {
                    if (!proceed) {
                        return null;
                    }
                    cached = distribute(cached, it.next());
                }
                break;
            case AND:
                nops = new LinkedHashSet<>();
                for (final Formula op : formula) {
                    final Formula apply = applyRec(op);
                    if (!proceed) {
                        return null;
                    }
                    nops.add(apply);
                }
                cached = f.and(nops);
                break;
            case PBC:
                cached = formula.nnf(f);
                break;
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.type());
        }
        if (proceed) {
            setCache(formula, cached);
            return cached;
        }
        return null;
    }

    /**
     * Computes the distribution (factorization) of two formulas.
     * @param f1 the first formula
     * @param f2 the second formula
     * @return the distribution of the two formulas
     */
    private Formula distribute(final Formula f1, final Formula f2) {
        if (handler != null) {
            proceed = handler.shouldResume(DISTRIBUTION_PERFORMED);
        }
        if (proceed) {
            if (f1.type() == FType.AND || f2.type() == FType.AND) {
                final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
                for (final Formula op : f1.type() == FType.AND ? f1 : f2) {
                    final Formula distribute = distribute(op, f1.type() == FType.AND ? f2 : f1);
                    if (!proceed) {
                        return null;
                    }
                    nops.add(distribute);
                }
                return f.and(nops);
            }
            final Formula clause = f.or(f1, f2);
            if (handler != null) {
                proceed = handler.shouldResume(new FactorizationCreatedClauseEvent(clause));
            }
            return clause;
        }
        return null;
    }
}
