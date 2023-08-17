// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.DEPTH;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;

/**
 * A function that computes the depth of a formula. The depth of an atomic formula
 * is defined as 0, all other operators increase the depth by 1.
 * @version 3.0.0
 * @since 2.0
 */
public final class FormulaDepthFunction implements FormulaFunction<Integer> {

    private static final FormulaDepthFunction CACHING_INSTANCE = new FormulaDepthFunction(true);
    private static final FormulaDepthFunction NON_CACHING_INSTANCE = new FormulaDepthFunction(false);

    private final boolean useCache;

    private FormulaDepthFunction(final boolean useCache) {
        this.useCache = useCache;
    }

    public static FormulaDepthFunction get(final boolean useCache) {
        return useCache ? CACHING_INSTANCE : NON_CACHING_INSTANCE;
    }

    @Override
    public Integer apply(final Formula formula) {
        final Object cached = formula.functionCacheEntry(DEPTH);
        if (cached != null) {
            return (Integer) cached;
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
        if (useCache) {
            formula.setFunctionCacheEntry(DEPTH, result);
        }
        return result;
    }
}
