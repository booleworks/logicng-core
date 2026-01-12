// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

public class LngNativeLiteral extends LngNativeFormula implements Literal {

    private final String name;
    private final boolean phase;
    private volatile int hashCode;

    /**
     * Constructor. A literal always has a name and a phase. A positive literal
     * can also be constructed directly as a {@link Variable}.
     * @param name  the literal name
     * @param phase the phase of the literal (also found as sign or polarity in
     *              the literature)
     * @param f     the factory which created this literal
     */
    LngNativeLiteral(final String name, final boolean phase, final NonCachingFormulaFactory f) {
        super(FType.LITERAL, f);
        this.name = name;
        this.phase = phase;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getPhase() {
        return phase;
    }

    @Override
    public Variable variable() {
        return f.variable(name);
    }

    @Override
    public Literal negate(final FormulaFactory f) {
        return f.literal(name, !phase);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            final int nameHash = name.hashCode();
            hashCode = getPhase() ? nameHash : nameHash * nameHash;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Literal && hashCode() == other.hashCode()) {
            final Literal otherLit = (Literal) other;
            return getPhase() == otherLit.getPhase() && getName().equals(otherLit.getName());
        }
        return false;
    }
}
