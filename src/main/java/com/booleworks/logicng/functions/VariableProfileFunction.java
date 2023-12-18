// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A function that computes the variable profile for a given formula, i.e. it
 * counts the number of occurrences for each variable.
 * <p>
 * For this function, the non-caching version is preferred since it usually
 * performs better. The non-caching version of this function generates the
 * result mapping only once and fills it recursively whereas the caching version
 * has to construct a new mapping for each sub-formula.
 * @version 3.0.0
 * @since 1.0
 */
public final class VariableProfileFunction extends CacheableFormulaFunction<Map<Variable, Integer>> {

    /**
     * Constructs a new function. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public VariableProfileFunction(final FormulaFactory f) {
        super(f, FunctionCacheEntry.VARPROFILE);
    }

    /**
     * Constructs a new function. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public VariableProfileFunction(final FormulaFactory f, final Map<Formula, Map<Variable, Integer>> cache) {
        super(f, cache);
    }

    @Override
    public Map<Variable, Integer> apply(final Formula formula) {
        return hasCache() ? cachingVariableProfile(formula) : nonCachingVariableProfile(formula);
    }

    /**
     * The non-caching implementation of the variable profile computation. In
     * this case the result map is only constructed once and results are just
     * added to it.
     * @param formula the formula
     * @return the variable profile
     */
    private Map<Variable, Integer> nonCachingVariableProfile(final Formula formula) {
        final SortedMap<Variable, Integer> map = new TreeMap<>();
        nonCachingRecursion(formula, map);
        return map;
    }

    /**
     * Recursive function for the non-caching variable profile computation.
     * @param formula the formula
     * @param map     the variable profile
     */
    private void nonCachingRecursion(final Formula formula, final Map<Variable, Integer> map) {
        if (formula.type() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            map.merge(lit.variable(), 1, Integer::sum);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals(f)) {
                nonCachingRecursion(l.variable(), map);
            }
        } else {
            for (final Formula op : formula) {
                nonCachingRecursion(op, map);
            }
        }
    }

    /**
     * The caching implementation of the variable profile computation. In this
     * case a result map is constructed for each sub-formula.
     * @param formula the formula
     * @return the variable profile
     */
    private Map<Variable, Integer> cachingVariableProfile(final Formula formula) {
        final Map<Variable, Integer> cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final Map<Variable, Integer> result = new HashMap<>();
        if (formula.type() == FType.LITERAL) {
            result.put(((Literal) formula).variable(), 1);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals(f)) {
                result.put(l.variable(), 1);
            }
        } else {
            for (final Formula op : formula) {
                final Map<Variable, Integer> temp = cachingVariableProfile(op);
                for (final Map.Entry<Variable, Integer> entry : temp.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        setCache(formula, result);
        return result;
    }
}
