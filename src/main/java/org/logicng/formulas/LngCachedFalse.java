// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

public class LngCachedFalse extends LngCachedFormula implements CFalse {

    /**
     * Constructor.
     * @param factory the factory which created this instance
     */
    LngCachedFalse(final CachingFormulaFactory factory) {
        super(FType.FALSE, factory);
    }

    @Override
    public int hashCode() {
        return -42;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof CFalse;
    }
}
