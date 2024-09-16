// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.implementation.noncaching.LngNativeVariable;

/**
 * An auxiliary variable for encoding results.
 * <p>
 * This variable is used, if the result is added directly to a solver. In this
 * case no variable on the factory has to be created.
 * @version 3.0.0
 * @since 1.1
 */
final class EncodingAuxiliaryVariable extends LngNativeVariable {

    final boolean negated;

    /**
     * Constructs a new auxiliary variable
     * @param name    the literal name
     * @param negated {@code true} if the variables is negated, {@code false}
     *                otherwise
     */
    EncodingAuxiliaryVariable(final String name, final boolean negated) {
        super(name, null);
        this.negated = negated;
    }

    @Override
    public Literal negate(final FormulaFactory f) {
        return new EncodingAuxiliaryVariable(getName(), !negated);
    }

    @Override
    public String toString() {
        return getName();
    }
}
