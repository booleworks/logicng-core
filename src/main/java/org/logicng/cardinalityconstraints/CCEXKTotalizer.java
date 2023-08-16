// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.Variable;

/**
 * Encodes that exactly 'rhs' variables can be assigned value true.  Uses the totalizer encoding for
 * translating the cardinality constraint into CNF.
 * @version 3.0.0
 * @since 1.1
 */
public final class CCEXKTotalizer implements CCExactlyK {

    private static final CCEXKTotalizer INSTANCE = new CCEXKTotalizer();

    private CCEXKTotalizer() {
        // Singleton pattern
    }

    public static CCEXKTotalizer get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final Variable[] vars, final int rhs) {
        CCTotalizer.buildEXK(result, vars, rhs);
    }
}
