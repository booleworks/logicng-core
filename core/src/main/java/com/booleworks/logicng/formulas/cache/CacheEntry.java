// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.cache;

/**
 * Interface for a cache entry in the formula cache.
 * @version 3.0.0
 * @since 1.0
 */
public interface CacheEntry {
    /**
     * Returns the description for this entry.
     * @return the description for this entry
     */
    String getDescription();
}
