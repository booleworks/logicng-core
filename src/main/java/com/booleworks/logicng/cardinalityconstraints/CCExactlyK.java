// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;

/**
 * The interface for exactly-k (ALK) cardinality constraints.
 * @version 3.0.0
 * @since 1.1
 */
public interface CCExactlyK {
    /**
     * Builds a cardinality constraint of the form
     * {@code var_1 + var_2 + ... + var_n = k}.
     * @param result the result of the encoding
     * @param vars   the variables {@code var_1 ... var_n}
     * @param rhs    the right-hand side {@code k} of the constraint
     * @throws IllegalArgumentException if the right-hand side of the
     *                                  cardinality constraint is negative
     */
    void build(final EncodingResult result, final Variable[] vars, int rhs);
}
