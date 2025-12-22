// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

import java.math.BigInteger;

/**
 * An entry in the BDD cache.
 * @version 1.4.0
 * @since 1.4.0
 */
final class BddCacheEntry {
    int a;
    int b;
    int c;
    BigInteger bdres;
    int res;

    /**
     * Constructs a new BDD cache entry.
     */
    BddCacheEntry() {
        reset();
    }

    /**
     * Resets this BDD cache entry.
     */
    void reset() {
        a = -1;
    }
}
