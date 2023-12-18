// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFunction;
import com.booleworks.logicng.formulas.cache.CacheEntry;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;

import java.util.Map;

/**
 * A cacheable formula function does not hold an internal mutable state but does
 * use an internal cache to speed up computations. This cache is usually changed
 * by the function. Formulas from a caching formula factory provide their cache
 * via the factory, formulas from a non-caching formula factory can be given
 * their own cache per function. A cacheable formula function with a provided
 * cache is not thread-safe.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class CacheableFormulaFunction<T> implements FormulaFunction<T> {
    protected final FormulaFactory f;
    private final Map<Formula, T> cache;

    /**
     * Creates a new cacheable formula function. For a caching formula factory,
     * the cache for the cache entry will be used, for a non-caching formula
     * factory no cache will be used.
     * @param f          the formula factory to generate new formulas
     * @param cacheEntry the type for the function cache entries in a caching
     *                   formula factory
     **/
    protected CacheableFormulaFunction(final FormulaFactory f, final CacheEntry cacheEntry) {
        this(f, f instanceof CachingFormulaFactory ? ((CachingFormulaFactory) f).getFunctionCacheForType(cacheEntry) :
                null);
    }

    /**
     * Creates a new cacheable formula function with a given cache. This cache
     * will always be used - even it the factory is caching and brings its own
     * cache it is ignored in this case.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the function (if null, none will be
     *              used)
     */
    public CacheableFormulaFunction(final FormulaFactory f, final Map<Formula, T> cache) {
        this.f = f;
        this.cache = cache;
    }

    protected void setCache(final Formula key, final T value) {
        if (cache != null) {
            cache.put(key, value);
        }
    }

    protected T lookupCache(final Formula formula) {
        if (cache == null) {
            return null;
        } else {
            return cache.get(formula);
        }
    }

    protected boolean hasCache() {
        return cache != null;
    }
}
