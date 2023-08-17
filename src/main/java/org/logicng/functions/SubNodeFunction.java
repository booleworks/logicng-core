// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.SUBFORMULAS;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;

import java.util.LinkedHashSet;

/**
 * A function that computes all sub-nodes of a given formula.  The order of the sub-nodes is bottom-up, i.e. a
 * sub-node only appears in the result when all of its sub-nodes are already listed.
 * @version 3.0.0
 * @since 1.0
 */
public final class SubNodeFunction implements FormulaFunction<LinkedHashSet<Formula>> {

    private static final SubNodeFunction CACHING_INSTANCE = new SubNodeFunction(true);
    private static final SubNodeFunction NON_CACHING_INSTANCE = new SubNodeFunction(false);

    private final boolean useCache;

    private SubNodeFunction(final boolean useCache) {
        this.useCache = useCache;
    }

    public static SubNodeFunction get(final boolean useCache) {
        return useCache ? CACHING_INSTANCE : NON_CACHING_INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LinkedHashSet<Formula> apply(final Formula formula) {
        final Object cached = formula.functionCacheEntry(SUBFORMULAS);
        if (cached != null) {
            return (LinkedHashSet<Formula>) cached;
        }
        final LinkedHashSet<Formula> result = new LinkedHashSet<>();
        for (final Formula op : formula) {
            if (!result.contains(op)) {
                result.addAll(apply(op));
            }
        }
        result.add(formula);
        if (useCache) {
            formula.setFunctionCacheEntry(SUBFORMULAS, result);
        }
        return result;
    }
}
