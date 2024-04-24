// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;

/**
 * Encodes that at most 'rhs' variables are assigned value true. Uses the
 * cardinality network encoding due to Asín, Nieuwenhuis, Oliveras, and
 * Rodríguez-Carbonell .
 * @version 3.0.0
 * @since 1.1
 */
public final class CCAMKCardinalityNetwork implements CCAtMostK {

    private static final CCAMKCardinalityNetwork INSTANCE = new CCAMKCardinalityNetwork();

    private CCAMKCardinalityNetwork() {
        // Singleton pattern
    }

    public static CCAMKCardinalityNetwork get() {
        return INSTANCE;
    }

    @Override
    public CCIncrementalData build(final EncodingResult result, final Variable[] vars, final int rhs) {
        CCCardinalityNetworks.buildAMK(result, vars, rhs);
        return null;
    }

    /**
     * Builds the constraint for incremental usage.
     * @param result the result
     * @param vars   the variables
     * @param rhs    the right-hand side
     */
    CCIncrementalData buildForIncremental(final EncodingResult result, final Variable[] vars, final int rhs) {
        return CCCardinalityNetworks.buildAMKForIncremental(result, vars, rhs);
    }
}
