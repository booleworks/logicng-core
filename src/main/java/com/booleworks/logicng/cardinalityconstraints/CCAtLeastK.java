// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;

/**
 * The interface for at-least-k (ALK) cardinality constraints.
 * @version 3.0.0
 * @since 1.0
 */
public interface CCAtLeastK {
    /**
     * Builds a cardinality constraint of the form {@code var_1 + var_2 + ... + var_n >= k}.
     * @param result the result of the encoding
     * @param vars   the variables {@code var_1 ... var_n}
     * @param rhs    the right-hand side {@code k} of the constraint
     * @return the incremental data for the constraintyy
     * @throws IllegalArgumentException if the right-hand side of the cardinality constraint is negative
     */
    CCIncrementalData build(final EncodingResult result, final Variable[] vars, int rhs);
}
