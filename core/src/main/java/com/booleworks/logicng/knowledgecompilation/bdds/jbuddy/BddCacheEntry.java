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
