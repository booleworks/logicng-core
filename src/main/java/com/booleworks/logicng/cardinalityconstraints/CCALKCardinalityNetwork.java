// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;

/**
 * Encodes that at least 'rhs' variables are assigned value true.  Uses the cardinality network
 * encoding due to Asín, Nieuwenhuis, Oliveras, and Rodríguez-Carbonell .
 * @version 3.0.0
 * @since 1.1
 */
public final class CCALKCardinalityNetwork implements CCAtLeastK {

    private static final CCALKCardinalityNetwork INSTANCE = new CCALKCardinalityNetwork();

    private CCALKCardinalityNetwork() {
        // Singleton pattern
    }

    public static CCALKCardinalityNetwork get() {
        return INSTANCE;
    }

    @Override
    public CCIncrementalData build(final EncodingResult result, final Variable[] vars, final int rhs) {
        CCCardinalityNetworks.buildALK(result, vars, rhs);
        return null;
    }

    /**
     * Builds the constraint for incremental usage.
     * @param result the result
     * @param vars   the variables
     * @param rhs    the right-hand side
     */
    CCIncrementalData buildForIncremental(final EncodingResult result, final Variable[] vars, final int rhs) {
        return CCCardinalityNetworks.buildALKForIncremental(result, vars, rhs);
    }
}
