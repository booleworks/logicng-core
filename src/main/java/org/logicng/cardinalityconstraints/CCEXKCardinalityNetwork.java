// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.Variable;

/**
 * Encodes that exactly 'rhs' variables are assigned value true.  Uses the cardinality network
 * encoding due to Asín, Nieuwenhuis, Oliveras, and Rodríguez-Carbonell .
 * @version 3.0.0
 * @since 1.1
 */
public final class CCEXKCardinalityNetwork implements CCExactlyK {

    private static final CCEXKCardinalityNetwork INSTANCE = new CCEXKCardinalityNetwork();

    private CCEXKCardinalityNetwork() {
        // Singleton pattern
    }

    public static CCEXKCardinalityNetwork get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final Variable[] vars, final int rhs) {
        CCCardinalityNetworks.buildEXK(result, vars, rhs);
    }
}
