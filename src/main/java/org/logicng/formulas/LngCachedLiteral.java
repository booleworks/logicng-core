// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

public class LngCachedLiteral extends LngCachedFormula implements Literal {

    private final String name;
    private final boolean phase;
    private final Variable var;
    private volatile Literal negated;
    private volatile int hashCode;

    /**
     * Constructor.  A literal always has a name and a phase.  A positive literal can also
     * be constructed directly as a {@link Variable}.
     * @param name  the literal name
     * @param phase the phase of the literal (also found as sign or polarity in the literature)
     * @param f     the factory which created this literal
     */
    LngCachedLiteral(final String name, final boolean phase, final CachingFormulaFactory f) {
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
        if (negated != null) {
            return negated;
        }
        negated = f.literal(name, !phase);
        return negated;
    }

    @Override
    public int hashCode() {
        final int result = hashCode;
        if (result == 0) {
            hashCode = name.hashCode() ^ (phase ? 1 : 0);
        }
        return hashCode;
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
