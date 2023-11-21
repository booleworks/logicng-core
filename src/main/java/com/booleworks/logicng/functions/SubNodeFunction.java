// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;

import java.util.LinkedHashSet;
import java.util.Map;

/**
 * A function that computes all sub-nodes of a given formula.  The order of the sub-nodes is bottom-up, i.e. a
 * sub-node only appears in the result when all of its sub-nodes are already listed.
 * @version 3.0.0
 * @since 1.0
 */
public final class SubNodeFunction extends CacheableFormulaFunction<LinkedHashSet<Formula>> {

    /**
     * Constructs a new function.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public SubNodeFunction(final FormulaFactory f) {
        super(f, FunctionCacheEntry.SUBFORMULAS);
    }

    /**
     * Constructs a new function.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public SubNodeFunction(final FormulaFactory f, final Map<Formula, LinkedHashSet<Formula>> cache) {
        super(f, cache);
    }

    @Override
    public LinkedHashSet<Formula> apply(final Formula formula) {
        final LinkedHashSet<Formula> cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final LinkedHashSet<Formula> result = new LinkedHashSet<>();
        for (final Formula op : formula) {
            if (!result.contains(op)) {
                result.addAll(apply(op));
            }
        }
        result.add(formula);
        setCache(formula, result);
        return result;
    }
}
