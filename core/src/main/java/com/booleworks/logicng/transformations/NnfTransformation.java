// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Transformation of a formula in NNF.
 * @version 3.0.0
 * @since 2.2.0
 */
public class NnfTransformation extends CacheableFormulaTransformation {

    /**
     * Constructs a new transformation. For a caching formula factory, the cache
     * of the factory will be used, for a non-caching formula factory no cache
     * will be used.
     * @param f the formula factory to generate new formulas
     */
    public NnfTransformation(final FormulaFactory f) {
        super(f, TransformationCacheEntry.NNF);
    }

    /**
     * Constructs a new transformation. For all factory type the provided cache
     * will be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache for this transformation
     */
    public NnfTransformation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(applyRec(formula, true));
    }

    private Formula applyRec(final Formula formula, final boolean polarity) {
        Formula nnf;
        if (polarity) {
            nnf = lookupCache(formula);
            if (nnf != null) {
                return nnf;
            }
        }
        final FType type = formula.getType();
        switch (type) {
            case TRUE:
            case FALSE:
            case LITERAL:
            case PREDICATE:
                nnf = polarity ? formula : formula.negate(f);
                break;
            case NOT:
                nnf = applyRec(((Not) formula).getOperand(), !polarity);
                break;
            case OR:
            case AND:
                nnf = applyRec(f, formula.iterator(), formula.getType(), polarity);
                break;
            case EQUIV:
                final Equivalence equiv = (Equivalence) formula;
                if (polarity) {
                    nnf = f.and(f.or(applyRec(equiv.getLeft(), false), applyRec(equiv.getRight(), true)),
                            f.or(applyRec(equiv.getLeft(), true), applyRec(equiv.getRight(), false)));
                } else {
                    nnf = f.and(f.or(applyRec(equiv.getLeft(), false), applyRec(equiv.getRight(), false)),
                            f.or(applyRec(equiv.getLeft(), true), applyRec(equiv.getRight(), true)));
                }
                break;
            case IMPL:
                final Implication impl = (Implication) formula;
                if (polarity) {
                    nnf = f.or(applyRec(impl.getLeft(), false), applyRec(impl.getRight(), true));
                } else {
                    nnf = f.and(applyRec(impl.getLeft(), true), applyRec(impl.getRight(), false));
                }
                break;
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                if (polarity) {
                    final List<Formula> encoding = pbc.getEncoding(f);
                    nnf = applyRec(f, encoding.iterator(), FType.AND, true);
                } else {
                    nnf = applyRec(pbc.negate(f), true);
                }
                break;
            default:
                throw new IllegalStateException("Unknown formula type = " + type);
        }
        if (polarity) {
            setCache(formula, nnf);
        }
        return nnf;
    }

    private Formula applyRec(final FormulaFactory f, final Iterator<Formula> formulas, final FType type,
                             final boolean polarity) {
        final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
        while (formulas.hasNext()) {
            final Formula formula = formulas.next();
            nops.add(applyRec(formula, polarity));
        }
        return f.naryOperator(polarity ? type : FType.dual(type), nops);
    }
}
