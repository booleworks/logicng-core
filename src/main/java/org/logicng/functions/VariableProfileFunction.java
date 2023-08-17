// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.VARPROFILE;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A function that computes the variable profile for a given formula, i.e. it counts the number of occurrences for
 * each variable.
 * <p>
 * For this function, the non-caching version is preferred since it usually performs better.  The non-caching version
 * of this function generates the result mapping only once and fills it recursively whereas the caching version has to
 * construct a new mapping for each sub-formula.
 * @version 3.0.0
 * @since 1.0
 */
public final class VariableProfileFunction implements FormulaFunction<Map<Variable, Integer>> {

    private static final VariableProfileFunction CACHING_INSTANCE = new VariableProfileFunction(true);
    private static final VariableProfileFunction NON_CACHING_INSTANCE = new VariableProfileFunction(false);

    private final boolean useCache;

    private VariableProfileFunction(final boolean useCache) {
        this.useCache = useCache;
    }

    public static VariableProfileFunction get(final boolean useCache) {
        return useCache ? CACHING_INSTANCE : NON_CACHING_INSTANCE;
    }

    @Override
    public Map<Variable, Integer> apply(final Formula formula) {
        return useCache ? cachingVariableProfile(formula) : nonCachingVariableProfile(formula);
    }

    /**
     * The non-caching implementation of the variable profile computation.  In this case the result map is only
     * constructed once and results are just added to it.
     * @param formula the formula
     * @return the variable profile
     */
    private static Map<Variable, Integer> nonCachingVariableProfile(final Formula formula) {
        final SortedMap<Variable, Integer> map = new TreeMap<>();
        nonCachingRecursion(formula, map);
        return map;
    }

    /**
     * Recursive function for the non-caching variable profile computation.
     * @param formula the formula
     * @param map     the variable profile
     */
    private static void nonCachingRecursion(final Formula formula, final Map<Variable, Integer> map) {
        if (formula.type() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            map.merge(lit.variable(), 1, Integer::sum);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals()) {
                nonCachingRecursion(l.variable(), map);
            }
        } else {
            for (final Formula op : formula) {
                nonCachingRecursion(op, map);
            }
        }
    }

    /**
     * The caching implementation of the variable profile computation.  In this case a result map is constructed for
     * each sub-formula.
     * @param formula the formula
     * @return the variable profile
     */
    @SuppressWarnings("unchecked")
    private static Map<Variable, Integer> cachingVariableProfile(final Formula formula) {
        final Object cached = formula.functionCacheEntry(VARPROFILE);
        if (cached != null) {
            return (Map<Variable, Integer>) cached;
        }
        final Map<Variable, Integer> result = new HashMap<>();
        if (formula.type() == FType.LITERAL) {
            result.put(((Literal) formula).variable(), 1);
        } else if (formula.type() == FType.PBC) {
            for (final Literal l : formula.literals()) {
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
        formula.setFunctionCacheEntry(VARPROFILE, result);
        return result;
    }
}
