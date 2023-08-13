// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.noncaching;

import org.logicng.formulas.CFalse;
import org.logicng.formulas.FType;

public class LngNativeFalse extends LngNativeFormula implements CFalse {

    /**
     * Constructor.
     * @param factory the factory which created this instance
     */
    LngNativeFalse(final NonCachingFormulaFactory factory) {
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
