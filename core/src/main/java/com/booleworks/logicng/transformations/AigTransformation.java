// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.LinkedHashSet;
import java.util.Map;

/**
 * And-inverter-graph (AIG) transformation. Returns the AIG of the given
 * formula.
 * @version 3.0.0
 * @since 1.0
 */
public class AigTransformation extends CacheableFormulaTransformation {

    /**
     * Constructs a new transformation. For a caching formula factory, the cache
     * of the factory will be used, for a non-caching formula factory no cache
     * will be used.
     * @param f the formula factory to generate new formulas
     */
    public AigTransformation(final FormulaFactory f) {
        super(f, TransformationCacheEntry.AIG);
    }

    /**
     * Constructs a new transformation. For all factory type the provided cache
     * will be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public AigTransformation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(transform(formula));
    }

    protected Formula transform(final Formula formula) {
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
                return formula;
            case NOT:
                return transformNot((Not) formula);
            case IMPL:
                return transformImplication((Implication) formula);
            case EQUIV:
                return transformEquivalence((Equivalence) formula);
            case AND:
                return transformAnd((And) formula);
            case OR:
                return transformOr((Or) formula);
            case PBC:
                return transform(formula.cnf(f));
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.getType());
        }
    }

    protected Formula transformNot(final Not not) {
        Formula aig = lookupCache(not);
        if (aig == null) {
            aig = f.not(apply(not.getOperand()));
            setCache(not, aig);
        }
        return aig;
    }

    protected Formula transformImplication(final Implication impl) {
        Formula aig = lookupCache(impl);
        if (aig == null) {
            aig = f.not(f.and(apply(impl.getLeft()), f.not(apply(impl.getRight()))));
            setCache(impl, aig);
        }
        return aig;
    }

    protected Formula transformEquivalence(final Equivalence equiv) {
        Formula aig = lookupCache(equiv);
        if (aig == null) {
            aig = f.and(f.not(f.and(apply(equiv.getLeft()), f.not(apply(equiv.getRight())))),
                    f.not(f.and(f.not(equiv.getLeft()), equiv.getRight())));
            setCache(equiv, aig);
        }
        return aig;
    }

    protected Formula transformAnd(final And and) {
        Formula aig = lookupCache(and);
        if (aig == null) {
            final LinkedHashSet<Formula> nops = new LinkedHashSet<>(and.numberOfOperands());
            for (final Formula op : and) {
                nops.add(apply(op));
            }
            aig = f.and(nops);
            setCache(and, aig);
        }
        return aig;
    }

    protected Formula transformOr(final Or or) {
        Formula aig = lookupCache(or);
        if (aig == null) {
            final LinkedHashSet<Formula> nops = new LinkedHashSet<>(or.numberOfOperands());
            for (final Formula op : or) {
                nops.add(f.not(apply(op)));
            }
            aig = f.not(f.and(nops));
            setCache(or, aig);
        }
        return aig;
    }
}
