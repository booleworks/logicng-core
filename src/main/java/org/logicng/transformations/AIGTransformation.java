// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.logicng.formulas.cache.TransformationCacheEntry.AIG;

import org.logicng.formulas.And;
import org.logicng.formulas.Equivalence;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Implication;
import org.logicng.formulas.Not;
import org.logicng.formulas.Or;

import java.util.LinkedHashSet;
import java.util.Map;

/**
 * And-inverter-graph (AIG) transformation.  Returns the AIG of the given formula.
 * @version 3.0.0
 * @since 1.0
 */
public final class AIGTransformation extends CacheableFormulaTransformation {

    /**
     * Constructs a new transformation.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public AIGTransformation(final FormulaFactory f) {
        super(f, AIG);
    }

    /**
     * Constructs a new transformation.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public AIGTransformation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public Formula apply(final Formula formula) {
        switch (formula.type()) {
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
                return apply(formula.cnf(f));
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.type());
        }
    }

    private Formula transformNot(final Not not) {
        Formula aig = lookupCache(not);
        if (aig == null) {
            aig = f.not(apply(not.operand()));
            setCache(not, aig);
        }
        return aig;
    }

    private Formula transformImplication(final Implication impl) {
        Formula aig = lookupCache(impl);
        if (aig == null) {
            aig = f.not(f.and(apply(impl.left()), f.not(apply(impl.right()))));
            setCache(impl, aig);
        }
        return aig;
    }

    private Formula transformEquivalence(final Equivalence equiv) {
        Formula aig = lookupCache(equiv);
        if (aig == null) {
            aig = f.and(f.not(f.and(apply(equiv.left()), f.not(apply(equiv.right())))),
                    f.not(f.and(f.not(equiv.left()), equiv.right())));
            setCache(equiv, aig);
        }
        return aig;
    }

    private Formula transformAnd(final And and) {
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

    private Formula transformOr(final Or or) {
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
