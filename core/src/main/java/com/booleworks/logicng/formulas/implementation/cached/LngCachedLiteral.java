// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

public class LngCachedLiteral extends LngCachedFormula implements Literal {

    private final String name;
    private final boolean phase;
    private final Variable var;
    private volatile Literal negated;
    private volatile int hashCode;

    /**
     * Constructor. A literal always has a name and a phase. A positive literal
     * can also be constructed directly as a {@link Variable}.
     * @param name  the literal name
     * @param phase the phase of the literal (also found as sign or polarity in
     *              the literature)
     * @param f     the factory which created this literal
     */
    LngCachedLiteral(final String name, final boolean phase, final CachingFormulaFactory f) {
        super(FType.LITERAL, f);
        this.name = name;
        this.phase = phase;
        var = phase ? (Variable) this : (Variable) negate(f);
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
        return var;
    }

    @Override
    public Literal negate(final FormulaFactory f) {
        if (negated != null) {
            return negated;
        }
        final Literal lit = f.literal(name, !phase);
        if (f == getFactory()) {
            negated = lit;
        }
        return lit;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = name.hashCode() ^ (phase ? 1 : 0);
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && getFactory() == ((Formula) other).getFactory()) {
            // the caching formula factory would have produced the same object
            return false;
        }
        if (other instanceof Literal && hashCode() == other.hashCode()) {
            final Literal otherLit = (Literal) other;
            return getPhase() == otherLit.getPhase() && getName().equals(otherLit.getName());
        }
        return false;
    }
}
