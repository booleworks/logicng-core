// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.Constant;
import com.booleworks.logicng.formulas.FType;

public abstract class LngCachedConstant extends LngCachedFormula implements Constant {

    /**
     * Constructor.
     * @param type the constant type
     * @param f    the factory which created this instance
     */
    LngCachedConstant(final FType type, final CachingFormulaFactory f) {
        super(type, f);
    }
}
