//  SPDX-License-Identifier: Apache-2.0 and MIT
//  Copyright 2015-2023 Christoph Zengler
//  Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.implementation.noncaching.LngNativeLiteral;

/**
 * An auxiliary literal for encoding results.
 * <p>
 * This literal is used, if the result is added directly to a solver. In this
 * case no variable on the factory has to be created.
 * @version 3.0.0
 * @since 3.0.0
 */
class EncodingAuxiliaryLiteral extends LngNativeLiteral {

    /**
     * Constructs a new auxiliary variable
     * @param name    the literal name
     * @param negated {@code true} if the variables is negated, {@code false}
     *                otherwise
     */
    EncodingAuxiliaryLiteral(final String name, final boolean negated) {
        super(name, !negated, null);
    }

    @Override
    public EncodingAuxiliaryVariable variable() {
        return new EncodingAuxiliaryVariable(getName());
    }

    @Override
    public Literal negate(final FormulaFactory f) {
        return new EncodingAuxiliaryLiteral(getName(), getPhase());
    }
}
