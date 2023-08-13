// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.noncaching;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.cache.CacheEntry;

public abstract class LngNativeFormula implements Formula {

    protected final NonCachingFormulaFactory f;
    protected FType type;

    protected LngNativeFormula(final FType type, final NonCachingFormulaFactory f) {
        this.f = f;
        this.type = type;
    }

    @Override
    public FType type() {
        return type;
    }

    @Override
    public NonCachingFormulaFactory factory() {
        return f;
    }

    @Override
    public Formula transformationCacheEntry(final CacheEntry key) {
        return null;
    }

    @Override
    public void setTransformationCacheEntry(final CacheEntry key, final Formula value) {
    }

    @Override
    public Tristate predicateCacheEntry(final CacheEntry key) {
        return null;
    }

    @Override
    public void setPredicateCacheEntry(final CacheEntry key, final boolean value) {
    }

    @Override
    public void setPredicateCacheEntry(final CacheEntry key, final Tristate value) {
    }

    @Override
    public Object functionCacheEntry(final CacheEntry key) {
        return null;
    }

    @Override
    public void setFunctionCacheEntry(final CacheEntry key, final Object value) {
    }

    @Override
    public void clearCaches() {
    }

    @Override
    public String toString() {
        return this.f.string(this);
    }
}
