// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;

/**
 * Encodes that at most one variable is assigned value true.  Uses the 'naive' encoding with no introduction
 * of new variables but quadratic size.
 * @version 3.0.0
 * @since 1.0
 */
public final class CCAMOPure implements CCAtMostOne {

    private static final CCAMOPure INSTANCE = new CCAMOPure();

    private CCAMOPure() {
        // Singleton pattern
    }

    public static CCAMOPure get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final CCConfig config, final Variable... vars) {
        result.reset();
        final FormulaFactory f = result.factory();
        for (int i = 0; i < vars.length; i++) {
            for (int j = i + 1; j < vars.length; j++) {
                result.addClause(vars[i].negate(f), vars[j].negate(f));
            }
        }
    }
}
