// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.CTrue;
import com.booleworks.logicng.formulas.FType;

public class LngCachedTrue extends LngCachedConstant implements CTrue {

    /**
     * Constructor.
     * @param factory the factory which created this instance
     */
    LngCachedTrue(final CachingFormulaFactory factory) {
        super(FType.TRUE, factory);
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof CTrue;
    }
}
