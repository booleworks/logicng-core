// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.cached;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.cache.CacheEntry;

public abstract class LngCachedFormula implements Formula {

    protected final CachingFormulaFactory f;
    protected FType type;

    protected LngCachedFormula(final FType type, final CachingFormulaFactory f) {
        this.f = f;
        this.type = type;
    }

    @Override
    public FType type() {
        return type;
    }

    @Override
    public CachingFormulaFactory factory() {
        return f;
    }

    @Override
    public Tristate predicateCacheEntry(final CacheEntry key) {
        return f.predicateCacheEntry(this, key);
    }

    @Override
    public void setPredicateCacheEntry(final CacheEntry key, final boolean value) {
        f.setPredicateCacheEntry(this, key, value);
    }

    @Override
    public void setPredicateCacheEntry(final CacheEntry key, final Tristate value) {
        f.setPredicateCacheEntry(this, key, value);
    }

    @Override
    public Object functionCacheEntry(final CacheEntry key) {
        return f.functionCacheEntry(this, key);
    }

    @Override
    public void setFunctionCacheEntry(final CacheEntry key, final Object value) {
        f.setFunctionCacheEntry(this, key, value);
    }

    @Override
    public void clearCaches() {
        f.clearCaches(this);
    }

    @Override
    public String toString() {
        return f.string(this);
    }
}
