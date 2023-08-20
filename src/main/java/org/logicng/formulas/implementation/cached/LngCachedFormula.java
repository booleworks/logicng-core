// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.cached;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;

public abstract class LngCachedFormula implements Formula {

    protected final CachingFormulaFactory f;
    protected FType type;

    protected LngCachedFormula(final FType type, final CachingFormulaFactory f) {
        this.f = f;
        this.type = type;
    }

    @Override
    public FType type() {
        return type;
    }

    @Override
    public CachingFormulaFactory factory() {
        return f;
    }

    @Override
    public String toString() {
        return f.string(this);
    }
}
