// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * ========================================================================
 * Copyright (C) 1996-2002 by Jorn Lind-Nielsen All rights reserved
 *
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, reproduce, prepare derivative works, distribute, and
 * display this software and its documentation for any purpose, provided that
 * (1) the above copyright notice and the following two paragraphs appear in all
 * copies of the source code and (2) redistributions, including without
 * limitation binaries, reproduce these notices in the supporting documentation.
 * Substantial modifications to this software may be copyrighted by their
 * authors and need not follow the licensing terms described here, provided that
 * the new terms are clearly indicated in all files where they apply.
 *
 * IN NO EVENT SHALL JORN LIND-NIELSEN, OR DISTRIBUTORS OF THIS SOFTWARE BE
 * LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR
 * CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 * DOCUMENTATION, EVEN IF THE AUTHORS OR ANY OF THE ABOVE PARTIES HAVE BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JORN LIND-NIELSEN SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS,
 * AND THE AUTHORS AND DISTRIBUTORS HAVE NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * ========================================================================
 */

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
