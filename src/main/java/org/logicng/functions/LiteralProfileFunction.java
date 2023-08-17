// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.LITPROFILE;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;
import org.logicng.formulas.Literal;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A function that computes the literal profile for a given formula, i.e. it counts the number of occurrences for
 * each literal.
 * <p>
 * For this function, the non-caching version is preferred since it usually performs better.  The non-caching version
 * of this function generates the result mapping only once and fills it recursively whereas the caching version has to
 * construct a new mapping for each sub-formula.
 * @version 3.0.0
 * @since 1.0
 */
public final class LiteralProfileFunction implements FormulaFunction<Map<Literal, Integer>> {

    private static final LiteralProfileFunction CACHING_INSTANCE = new LiteralProfileFunction(true);
    private static final LiteralProfileFunction NON_CACHING_INSTANCE = new LiteralProfileFunction(false);

    private final boolean useCache;

    private LiteralProfileFunction(final boolean useCache) {
        this.useCache = useCache;
    }

    public static LiteralProfileFunction get(final boolean useCache) {
        return useCache ? CACHING_INSTANCE : NON_CACHING_INSTANCE;
    }

    @Override
    public Map<Literal, Integer> apply(final Formula formula) {
        return useCache ? cachingLiteralProfile(formula) : nonCachingLiteralProfile(formula);
    }

    /**
     * The non-caching implementation of the literal profile computation.  In this case the result map is only
     * constructed once and results are just added to it.
     * @param formula the formula
     * @return the literal profile
     */
    private static Map<Literal, Integer> nonCachingLiteralProfile(final Formula formula) {
        final SortedMap<Literal, Integer> map = new TreeMap<>();
        nonCachingRecursion(formula, map);
        return map;
    }

    /**
     * Recursive function for the non-caching literal profile computation.
     * @param formula the formula
     * @param map     the literal profile
     */
    private static void nonCachingRecursion(final Formula formula, final Map<Literal, Integer> map) {
        if (formula.type() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            map.merge(lit, 1, Integer::sum);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals()) {
                nonCachingRecursion(l, map);
            }
        } else {
            for (final Formula op : formula) {
                nonCachingRecursion(op, map);
            }
        }
    }

    /**
     * The caching implementation of the literal profile computation.  In this case a result map is constructed for
     * each sub-formula.
     * @param formula the formula
     * @return the literal profile
     */
    @SuppressWarnings("unchecked")
    private static Map<Literal, Integer> cachingLiteralProfile(final Formula formula) {
        final Object cached = formula.functionCacheEntry(LITPROFILE);
        if (cached != null) {
            return (Map<Literal, Integer>) cached;
        }
        final Map<Literal, Integer> result = new HashMap<>();
        if (formula.type() == FType.LITERAL) {
            result.put((Literal) formula, 1);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals()) {
                result.put(l, 1);
            }
        } else {
            for (final Formula op : formula) {
                final Map<Literal, Integer> temp = cachingLiteralProfile(op);
                for (final Map.Entry<Literal, Integer> entry : temp.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        formula.setFunctionCacheEntry(LITPROFILE, result);
        return result;
    }
}
