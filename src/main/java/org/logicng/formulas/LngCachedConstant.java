// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

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
