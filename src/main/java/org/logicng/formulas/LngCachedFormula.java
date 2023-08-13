// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

public abstract class LngCachedFormula implements Formula {

    protected final FormulaFactory f;
    protected FType type;

    protected LngCachedFormula(final FType type, final FormulaFactory f) {
        this.f = f;
        this.type = type;
    }

    @Override
    public FType type() {
        return type;
    }

    @Override
    public FormulaFactory factory() {
        return f;
    }

    @Override
    public String toString() {
        return this.f.string(this);
    }
}
