// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.propositions;

import com.booleworks.logicng.formulas.Formula;

import java.util.Objects;

/**
 * A proposition in LogicNG.  A proposition is a formula with an additional textual description.
 * @version 2.0.0
 * @since 1.0
 */
public final class StandardProposition extends Proposition {

    private final Formula formula;
    private final String description;

    /**
     * Constructs a new proposition for a single formulas.
     * @param formula the formulas
     */
    public StandardProposition(final Formula formula) {
        this.formula = formula;
        description = "";
    }

    /**
     * Constructs a new proposition for a single formulas.
     * @param description the description
     * @param formula     the formulas
     */
    public StandardProposition(final String description, final Formula formula) {
        this.formula = formula;
        this.description = description == null ? "" : description;
    }

    @Override
    public Formula formula() {
        return formula;
    }

    /**
     * Returns the backpack of this proposition.
     * @return the backpack of this proposition
     */
    public String description() {
        return description;
    }

    @Override
    public int hashCode() {
        return Objects.hash(formula, description);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof StandardProposition) {
            final StandardProposition o = (StandardProposition) other;
            return Objects.equals(formula, o.formula) && Objects.equals(description, o.description);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("StandardProposition{formula=%s, description=%s}", formula, description);
    }
}
