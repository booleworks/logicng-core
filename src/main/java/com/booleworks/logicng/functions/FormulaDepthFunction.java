// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;

import java.util.Map;

/**
 * A function that computes the depth of a formula. The depth of an atomic formula
 * is defined as 0, all other operators increase the depth by 1.
 * @version 3.0.0
 * @since 2.0
 */
public final class FormulaDepthFunction extends CacheableFormulaFunction<Integer> {

    /**
     * Constructs a new function.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public FormulaDepthFunction(final FormulaFactory f) {
        super(f, FunctionCacheEntry.DEPTH);
    }

    /**
     * Constructs a new function.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public FormulaDepthFunction(final FormulaFactory f, final Map<Formula, Integer> cache) {
        super(f, cache);
    }

    @Override
    public Integer apply(final Formula formula) {
        final Integer cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final int result;
        if (formula.isAtomicFormula()) {
            result = 0;
        } else {
            int maxDepth = 0;
            for (final Formula op : formula) {
                maxDepth = Math.max(maxDepth, apply(op));
            }
            result = maxDepth + 1;
        }
        setCache(formula, result);
        return result;
    }
}
