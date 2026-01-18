// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.propositions;

import com.booleworks.logicng.formulas.Formula;

import java.util.Objects;

/**
 * An extended proposition in LogicNG. An extended proposition is a formula with
 * additional information of a user-provided type.
 * @param <T> the type of the backpack
 * @version 3.0.0
 * @since 1.0
 */
public class ExtendedProposition<T> extends Proposition {

    private final Formula formula;
    private final T backpack;

    /**
     * Constructs a new extended proposition for a single formula.
     * @param backpack the backpack
     * @param formula  the formula
     */
    public ExtendedProposition(final T backpack, final Formula formula) {
        this.formula = formula;
        this.backpack = backpack;
    }

    @Override
    public Formula getFormula() {
        return formula;
    }

    /**
     * Returns the backpack of this proposition.
     * @return the backpack of this proposition
     */
    public T getBackpack() {
        return backpack;
    }

    @Override
    public int hashCode() {
        return Objects.hash(formula, backpack);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ExtendedProposition) {
            final ExtendedProposition<?> o = (ExtendedProposition<?>) other;
            return Objects.equals(formula, o.formula) && Objects.equals(backpack, o.backpack);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("ExtendedProposition{formula=%s, backpack=%s}", formula, backpack);
    }
}
