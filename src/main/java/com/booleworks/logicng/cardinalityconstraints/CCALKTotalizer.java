// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;

/**
 * Encodes that at least 'rhs' variables are assigned value true.  Uses the totalizer encoding for
 * translating the cardinality constraint into CNF.
 * @version 3.0.0
 * @since 1.0
 */
public final class CCALKTotalizer implements CCAtLeastK {

    private static final CCALKTotalizer INSTANCE = new CCALKTotalizer();

    private CCALKTotalizer() {
        // Singleton pattern
    }

    public static CCALKTotalizer get() {
        return INSTANCE;
    }

    @Override
    public CCIncrementalData build(final EncodingResult result, final Variable[] vars, final int rhs) {
        return CCTotalizer.buildALK(result, vars, rhs);
    }
}
