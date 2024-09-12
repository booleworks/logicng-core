// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A function that computes the literal profile for a given formula, i.e. it
 * counts the number of occurrences for each literal.
 * <p>
 * For this function, the non-caching version is preferred since it usually
 * performs better. The non-caching version of this function generates the
 * result mapping only once and fills it recursively whereas the caching version
 * has to construct a new mapping for each sub-formula.
 * @version 3.0.0
 * @since 1.0
 */
public final class LiteralProfileFunction extends CacheableFormulaFunction<Map<Literal, Integer>> {

    /**
     * Constructs a new function. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public LiteralProfileFunction(final FormulaFactory f) {
        super(f, FunctionCacheEntry.LITPROFILE);
    }

    /**
     * Constructs a new function. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public LiteralProfileFunction(final FormulaFactory f, final Map<Formula, Map<Literal, Integer>> cache) {
        super(f, cache);
    }

    @Override
    public LNGResult<Map<Literal, Integer>> apply(final Formula formula, ComputationHandler handler) {
        return LNGResult.of(hasCache() ? cachingLiteralProfile(formula) : nonCachingLiteralProfile(formula));
    }

    /**
     * The non-caching implementation of the literal profile computation. In
     * this case the result map is only constructed once and results are just
     * added to it.
     * @param formula the formula
     * @return the literal profile
     */
    private Map<Literal, Integer> nonCachingLiteralProfile(final Formula formula) {
        final SortedMap<Literal, Integer> map = new TreeMap<>();
        nonCachingRecursion(formula, map);
        return map;
    }

    /**
     * Recursive function for the non-caching literal profile computation.
     * @param formula the formula
     * @param map     the literal profile
     */
    private void nonCachingRecursion(final Formula formula, final Map<Literal, Integer> map) {
        if (formula.type() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            map.merge(lit, 1, Integer::sum);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals(f)) {
                nonCachingRecursion(l, map);
            }
        } else {
            for (final Formula op : formula) {
                nonCachingRecursion(op, map);
            }
        }
    }

    /**
     * The caching implementation of the literal profile computation. In this
     * case a result map is constructed for each sub-formula.
     * @param formula the formula
     * @return the literal profile
     */
    private Map<Literal, Integer> cachingLiteralProfile(final Formula formula) {
        final Map<Literal, Integer> cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final Map<Literal, Integer> result = new HashMap<>();
        if (formula.type() == FType.LITERAL) {
            result.put((Literal) formula, 1);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals(f)) {
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
        setCache(formula, result);
        return result;
    }
}
