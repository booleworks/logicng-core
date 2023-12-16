// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;

public abstract class LngNativeFormula implements Formula {

    protected final NonCachingFormulaFactory f;
    protected final FType type;

    protected LngNativeFormula(final FType type, final NonCachingFormulaFactory f) {
        this.f = f;
        this.type = type;
    }

    @Override
    public FType type() {
        return type;
    }

    @Override
    public NonCachingFormulaFactory factory() {
        return f;
    }

    @Override
    public String toString() {
        return f.string(this);
    }
}
