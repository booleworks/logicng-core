// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Literal;

/**
 * The interface for pseudo Boolean constraint encodings.
 * @version 3.0.0
 * @since 1.1
 */
public interface PBEncoding {

    /**
     * Encodes a pseudo-Boolean constraint in the given encoding result
     * @param result the result of the encoding
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraint
     * @param rhs    the right-hand side of the constraint
     */
    default void encode(final EncodingResult result, final LNGVector<Literal> lits, final LNGIntVector coeffs,
                        final int rhs) {
        encode(result, lits, coeffs, rhs, (PBConfig) result.factory().configurationFor(ConfigurationType.PB_ENCODER));
    }

    /**
     * Encodes a pseudo-Boolean constraint in the given encoding result
     * @param result the result of the encoding
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraint
     * @param rhs    the right-hand side of the constraint
     * @param config the configuration for the encoding
     */
    void encode(final EncodingResult result, final LNGVector<Literal> lits, final LNGIntVector coeffs, int rhs,
                final PBConfig config);
}
