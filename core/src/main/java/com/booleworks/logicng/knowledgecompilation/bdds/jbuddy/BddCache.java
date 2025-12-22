// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

/**
 * BDD Cache.
 * @version 3.0.0
 * @since 1.4.0
 */
public class BddCache {
    private final BddPrime prime;
    private BddCacheEntry[] table;

    /**
     * Constructs a new BDD cache of a given size (number of entries in the
     * cache).
     * @param cs the cache size
     */
    protected BddCache(final int cs) {
        prime = new BddPrime();
        resize(cs);
    }

    /**
     * Resizes the cache to a new number of entries. The old cache entries are
     * removed in this process.
     * @param ns the new number of entries
     */
    protected void resize(final int ns) {
        final int size = prime.primeGte(ns);
        table = new BddCacheEntry[size];
        for (int n = 0; n < size; n++) {
            table[n] = new BddCacheEntry();
        }
    }

    /**
     * Resets (clears) the cache.
     */
    protected void reset() {
        for (final BddCacheEntry ce : table) {
            ce.reset();
        }
    }

    /**
     * Looks up a given hash value in the cache and returns the respective cache
     * entry.
     * <p>
     * The return value is guaranteed to be non-null since every entry in the
     * cache is always a {@link BddCacheEntry} object.
     * @param hash the hash value.
     * @return the respective entry in the cache
     */
    BddCacheEntry lookup(final int hash) {
        return table[Math.abs(hash % table.length)];
    }
}
