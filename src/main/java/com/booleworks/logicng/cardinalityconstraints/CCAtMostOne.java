// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * The interface for at-most-one (AMO) cardinality constraints.
 * @version 3.0.0
 * @since 1.0
 */
public interface CCAtMostOne {

    /**
     * Builds a cardinality constraint of the form {@code var_1 + var_2 + ... + var_n <= 1}.
     * @param result the result for the encoding
     * @param vars   the variables {@code var_1 ... var_n}
     */
    default void build(final EncodingResult result, final Variable... vars) {
        build(result, (CCConfig) result.factory().configurationFor(ConfigurationType.CC_ENCODER), vars);
    }

    /**
     * Builds a cardinality constraint of the form {@code var_1 + var_2 + ... + var_n <= 1}.
     * @param result the result for the encoding
     * @param config the configuration for the encoding
     * @param vars   the variables {@code var_1 ... var_n}
     */
    void build(final EncodingResult result, final CCConfig config, final Variable... vars);

    static void encodeNaive(final EncodingResult result, final LNGVector<Literal> vars) {
        if (vars.size() > 1) {
            for (int i = 0; i < vars.size(); i++) {
                for (int j = i + 1; j < vars.size(); j++) {
                    result.addClause(vars.get(i).negate(result.factory()), vars.get(j).negate(result.factory()));
                }
            }
        }
    }
}
