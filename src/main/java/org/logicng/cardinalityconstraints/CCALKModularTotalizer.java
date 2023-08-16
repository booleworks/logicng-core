// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.Variable;

/**
 * Encodes that at least 'rhs' variables can be assigned value true.  Uses the modular totalizer encoding for
 * translating the cardinality constraint into CNF.
 * @version 3.0.0
 * @since 1.0
 */
public final class CCALKModularTotalizer implements CCAtLeastK {

    private static final CCALKModularTotalizer INSTANCE = new CCALKModularTotalizer();

    private CCALKModularTotalizer() {
        // Singleton pattern
    }

    public static CCALKModularTotalizer get() {
        return INSTANCE;
    }

    @Override
    public CCIncrementalData build(final EncodingResult result, final Variable[] vars, final int rhs) {
        return CCModularTotalizer.buildALK(result, vars, rhs);
    }
}
