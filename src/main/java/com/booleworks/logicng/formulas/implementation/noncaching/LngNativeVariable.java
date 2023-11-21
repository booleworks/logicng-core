// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.Variable;

public class LngNativeVariable extends LngNativeLiteral implements Variable {

    /**
     * Constructor.
     * @param name the literal name
     * @param f    the factory which created this literal
     */
    protected LngNativeVariable(final String name, final NonCachingFormulaFactory f) {
        super(name, true, f);
    }
}
