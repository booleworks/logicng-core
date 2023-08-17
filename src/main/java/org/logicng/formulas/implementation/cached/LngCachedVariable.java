// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.cached;

import org.logicng.formulas.Variable;

public class LngCachedVariable extends LngCachedLiteral implements Variable {

    /**
     * Constructor.
     * @param name the literal name
     * @param f    the factory which created this literal
     */
    protected LngCachedVariable(final String name, final CachingFormulaFactory f) {
        super(name, true, f);
    }
}
