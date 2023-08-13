// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.noncaching;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

public class LngNativeLiteral extends LngNativeFormula implements Literal {

    private final String name;
    private final boolean phase;
    private final Variable var;

    /**
     * Constructor.  A literal always has a name and a phase.  A positive literal can also
     * be constructed directly as a {@link Variable}.
     * @param name  the literal name
     * @param phase the phase of the literal (also found as sign or polarity in the literature)
     * @param f     the factory which created this literal
     */
    LngNativeLiteral(final String name, final boolean phase, final NonCachingFormulaFactory f) {
        super(FType.LITERAL, f);
        this.name = name;
        this.phase = phase;
        this.var = phase ? (Variable) this : (Variable) negate();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean phase() {
        return phase;
    }

    @Override
    public Variable variable() {
        return var;
    }

    @Override
    public Literal negate() {
        return f.literal(name, !phase);
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ (phase ? 1 : 0);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && factory() == ((Formula) other).factory()) {
            return false; // the same formula factory would have produced a == object
        }
        if (other instanceof Literal) {
            final Literal otherLit = (Literal) other;
            return phase() == otherLit.phase() && name().equals(otherLit.name());
        }
        return false;
    }
}
