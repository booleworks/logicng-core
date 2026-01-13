// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.encodingresult;

import com.booleworks.logicng.formulas.Variable;

/**
 * An auxiliary variable for encoding results.
 * <p>
 * This variable is used, if the result is added directly to a solver. In this
 * case no variable on the factory has to be created.
 * @version 3.0.0
 * @since 1.1
 */
class EncodingAuxiliaryVariable extends EncodingAuxiliaryLiteral implements Variable {

    /**
     * Constructs a new auxiliary variable
     * @param name the literal name
     */
    EncodingAuxiliaryVariable(final String name) {
        super(name, false);
    }

    @Override
    public EncodingAuxiliaryVariable variable() {
        return this;
    }

    @Override
    public String toString() {
        return getName();
    }
}
