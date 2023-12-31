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
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Transformation of a formula in NNF.
 * @version 3.0.0
 * @since 2.2.0
 */
public class NNFTransformation extends CacheableFormulaTransformation {

    /**
     * Constructs a new transformation. For a caching formula factory, the cache
     * of the factory will be used, for a non-caching formula factory no cache
     * will be used.
     * @param f the formula factory to generate new formulas
     */
    public NNFTransformation(final FormulaFactory f) {
        super(f, TransformationCacheEntry.NNF);
    }

    /**
     * Constructs a new transformation. For all factory type the provided cache
     * will be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache for this transformation
     */
    public NNFTransformation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public Formula apply(final Formula formula) {
        return applyRec(formula, true);
    }

    private Formula applyRec(final Formula formula, final boolean polarity) {
        Formula nnf;
        if (polarity) {
            nnf = lookupCache(formula);
            if (nnf != null) {
                return nnf;
            }
        }
        final FType type = formula.type();
        switch (type) {
            case TRUE:
            case FALSE:
            case LITERAL:
            case PREDICATE:
                nnf = polarity ? formula : formula.negate(f);
                break;
            case NOT:
                nnf = applyRec(((Not) formula).operand(), !polarity);
                break;
            case OR:
            case AND:
                nnf = applyRec(f, formula.iterator(), formula.type(), polarity);
                break;
            case EQUIV:
                final Equivalence equiv = (Equivalence) formula;
                if (polarity) {
                    nnf = f.and(f.or(applyRec(equiv.left(), false), applyRec(equiv.right(), true)),
                            f.or(applyRec(equiv.left(), true), applyRec(equiv.right(), false)));
                } else {
                    nnf = f.and(f.or(applyRec(equiv.left(), false), applyRec(equiv.right(), false)),
                            f.or(applyRec(equiv.left(), true), applyRec(equiv.right(), true)));
                }
                break;
            case IMPL:
                final Implication impl = (Implication) formula;
                if (polarity) {
                    nnf = f.or(applyRec(impl.left(), false), applyRec(impl.right(), true));
                } else {
                    nnf = f.and(applyRec(impl.left(), true), applyRec(impl.right(), false));
                }
                break;
            case PBC:
                final PBConstraint pbc = (PBConstraint) formula;
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
