// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaTransformation;
import com.booleworks.logicng.formulas.cache.CacheEntry;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;

import java.util.Map;

/**
 * A cacheable formula transformation does not hold an internal mutable state
 * but does use an internal cache to speed up computations. This cache is
 * usually changed by the transformation. Formulas from a caching formula
 * factory provide their cache via the factory, formulas from a non-caching
 * formula factory can be given their own cache per transformation. A cacheable
 * formula transformation with a provided cache is not thread-safe.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class CacheableFormulaTransformation implements FormulaTransformation {
    protected final FormulaFactory f;
    private final Map<Formula, Formula> cache;

    /**
     * Creates a new cacheable formula transformation. For a caching formula
     * factory, the cache for the cache entry will be used, for a non-caching
     * formula factory no cache will be used.
     * @param f          the formula factory to generate new formulas
     * @param cacheEntry the type for the transformation cache entries in a
     *                   caching formula factory
     **/
    protected CacheableFormulaTransformation(final FormulaFactory f, final CacheEntry cacheEntry) {
        this(f, f instanceof CachingFormulaFactory ?
                ((CachingFormulaFactory) f).getTransformationCacheForType(cacheEntry) : null);
    }

    /**
     * Creates a new cacheable formula transformation with a given cache. This
     * cache will always be used - even it the factory is caching and brings its
     * own cache it is ignored in this case.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation (if null, none will
     *              be used)
     */
    public CacheableFormulaTransformation(final FormulaFactory f, final Map<Formula, Formula> cache) {
        this.f = f;
        this.cache = cache;
    }

    protected void setCache(final Formula key, final Formula value) {
        if (cache != null) {
            cache.put(key, value);
        }
    }

    protected Formula lookupCache(final Formula formula) {
        if (cache == null) {
            return null;
        } else {
            return cache.get(formula);
        }
    }
}
