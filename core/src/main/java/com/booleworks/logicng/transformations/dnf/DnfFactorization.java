// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.dnf;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.FACTORIZATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DISTRIBUTION_PERFORMED;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.FactorizationCreatedClauseEvent;
import com.booleworks.logicng.transformations.CacheableFormulaTransformation;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Transformation of a formula in DNF by factorization.
 * @version 3.0.0
 * @since 1.0
 */
public final class DnfFactorization extends CacheableFormulaTransformation {

    /**
     * Constructor for a DNF Factorization.
     * @param f the formula factory to generate new formulas
     */
    public DnfFactorization(final FormulaFactory f) {
        super(f, TransformationCacheEntry.FACTORIZED_DNF);
    }

    /**
     * Constructor for a DNF Factorization.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public DnfFactorization(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        if (!handler.shouldResume(FACTORIZATION_STARTED)) {
            return LngResult.canceled(FACTORIZATION_STARTED);
        }
        return applyRec(formula, handler);
    }

    private LngResult<Formula> applyRec(final Formula formula, final ComputationHandler handler) {
        if (formula.getType().getPrecedence() >= FType.LITERAL.getPrecedence()) {
            return LngResult.of(formula);
        }
        final Formula cached = lookupCache(formula);
        if (cached != null) {
            return LngResult.of(cached);
        }
        final Formula computed;
        switch (formula.getType()) {
            case NOT:
            case IMPL:
            case EQUIV:
            case PBC:
                final LngResult<Formula> rec = applyRec(formula.nnf(f), handler);
                if (rec.isSuccess()) {
                    computed = rec.getResult();
                } else {
                    return rec;
                }
                break;
            case OR:
                LinkedHashSet<Formula> nops = new LinkedHashSet<>();
                for (final Formula op : formula) {
                    final LngResult<Formula> nop = applyRec(op, handler);
                    if (nop.isSuccess()) {
                        nops.add(nop.getResult());
                    } else {
                        return nop;
                    }
                }
                computed = f.or(nops);
                break;
            case AND:
                nops = new LinkedHashSet<>();
                for (final Formula op : formula) {
                    final LngResult<Formula> nop = applyRec(op, handler);
                    if (nop.isSuccess()) {
                        nops.add(nop.getResult());
                    } else {
                        return nop;
                    }
                }
                final Iterator<Formula> it = nops.iterator();
                Formula currentResult = it.next();
                while (it.hasNext()) {
                    final LngResult<Formula> distributed = distribute(currentResult, it.next(), handler);
                    if (distributed.isSuccess()) {
                        currentResult = distributed.getResult();
                    } else {
                        return distributed;
                    }
                }
                computed = currentResult;
                break;
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.getType());
        }
        setCache(formula, computed);
        return LngResult.of(computed);
    }

    /**
     * Computes the distribution (factorization) of two formulas.
     * @param f1      the first formula
     * @param f2      the second formula
     * @param handler the computation handler
     * @return the distribution of the two formulas
     */
    private LngResult<Formula> distribute(final Formula f1, final Formula f2, final ComputationHandler handler) {
        if (!handler.shouldResume(DISTRIBUTION_PERFORMED)) {
            return LngResult.canceled(DISTRIBUTION_PERFORMED);
        }
        if (f1.getType() == FType.OR || f2.getType() == FType.OR) {
            final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
            for (final Formula op : f1.getType() == FType.OR ? f1 : f2) {
                final LngResult<Formula> distributed = distribute(op, f1.getType() == FType.OR ? f2 : f1, handler);
                if (distributed.isSuccess()) {
                    nops.add(distributed.getResult());
                } else {
                    return distributed;
                }
            }
            return LngResult.of(f.or(nops));
        }
        final Formula clause = f.and(f1, f2);
        final FactorizationCreatedClauseEvent createdClauseEvent = new FactorizationCreatedClauseEvent(clause);
        if (handler.shouldResume(createdClauseEvent)) {
            return LngResult.of(clause);
        } else {
            return LngResult.canceled(createdClauseEvent);
        }
    }
}
