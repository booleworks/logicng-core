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
import org.logicng.formulas.cache.PredicateCacheEntry;

import java.util.LinkedHashSet;

/**
 * And-inverter-graph (AIG) transformation.  Returns the AIG of the given formula.
 * @version 3.0.0
 * @since 1.0
 */
public final class AIGTransformation extends StatelessFormulaTransformation {

    private final boolean useCache;

    public AIGTransformation(final FormulaFactory f) {
        this(f, true);
    }

    public AIGTransformation(final FormulaFactory f, final boolean useCache) {
        super(f);
        this.useCache = useCache;
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
                return apply(formula.cnf());
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.type());
        }
    }

    private Formula transformNot(final Not not) {
        Formula aig = not.transformationCacheEntry(AIG);
        if (aig == null) {
            aig = f.not(apply(not.operand()));
            if (useCache) {
                not.setTransformationCacheEntry(AIG, aig);
                aig.setPredicateCacheEntry(PredicateCacheEntry.IS_AIG, true);
            }
        }
        return aig;
    }

    private Formula transformImplication(final Implication impl) {
        Formula aig = impl.transformationCacheEntry(AIG);
        if (aig == null) {
            aig = f.not(f.and(apply(impl.left()), f.not(apply(impl.right()))));
            if (useCache) {
                impl.setTransformationCacheEntry(AIG, aig);
                aig.setPredicateCacheEntry(PredicateCacheEntry.IS_AIG, true);
            }
        }
        return aig;
    }

    private Formula transformEquivalence(final Equivalence equiv) {
        Formula aig = equiv.transformationCacheEntry(AIG);
        if (aig == null) {
            aig = f.and(f.not(f.and(apply(equiv.left()), f.not(apply(equiv.right())))),
                    f.not(f.and(f.not(equiv.left()), equiv.right())));
            if (useCache) {
                equiv.setTransformationCacheEntry(AIG, aig);
                aig.setPredicateCacheEntry(PredicateCacheEntry.IS_AIG, true);
            }
        }
        return aig;
    }

    private Formula transformAnd(final And and) {
        Formula aig = and.transformationCacheEntry(AIG);
        if (aig == null) {
            final LinkedHashSet<Formula> nops = new LinkedHashSet<>(and.numberOfOperands());
            for (final Formula op : and) {
                nops.add(apply(op));
            }
            aig = f.and(nops);
            if (useCache) {
                and.setTransformationCacheEntry(AIG, aig);
                aig.setPredicateCacheEntry(PredicateCacheEntry.IS_AIG, true);
            }
        }
        return aig;
    }

    private Formula transformOr(final Or or) {
        Formula aig = or.transformationCacheEntry(AIG);
        if (aig == null) {
            final LinkedHashSet<Formula> nops = new LinkedHashSet<>(or.numberOfOperands());
            for (final Formula op : or) {
                nops.add(f.not(apply(op)));
            }
            aig = f.not(f.and(nops));
            if (useCache) {
                or.setTransformationCacheEntry(AIG, aig);
                aig.setPredicateCacheEntry(PredicateCacheEntry.IS_AIG, true);
            }
        }
        return aig;
    }
}
