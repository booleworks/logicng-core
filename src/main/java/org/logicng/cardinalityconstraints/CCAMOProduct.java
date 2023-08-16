// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

/**
 * Encodes that at most one variable is assigned value true.  Uses the 2-product method due to Chen.
 * @version 3.0.0
 * @since 1.0
 */
public final class CCAMOProduct implements CCAtMostOne {

    private static final CCAMOProduct INSTANCE = new CCAMOProduct();

    private CCAMOProduct() {
        // Singleton pattern
    }

    public static CCAMOProduct get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final CCConfig config, final Variable... vars) {
        result.reset();
        final int recursiveBound = config.productRecursiveBound;
        productRec(result, recursiveBound, vars);
    }

    private static void productRec(final EncodingResult result, final int recursiveBound, final Variable... vars) {
        final FormulaFactory f = result.factory();
        final int n = vars.length;
        final int p = (int) Math.ceil(Math.sqrt(n));
        final int q = (int) Math.ceil((double) n / (double) p);
        final Variable[] us = new Variable[p];
        for (int i = 0; i < us.length; i++) {
            us[i] = result.newVariable();
        }
        final Variable[] vs = new Variable[q];
        for (int i = 0; i < vs.length; i++) {
            vs[i] = result.newVariable();
        }
        if (us.length <= recursiveBound) {
            buildPure(result, us);
        } else {
            productRec(result, recursiveBound, us);
        }
        if (vs.length <= recursiveBound) {
            buildPure(result, vs);
        } else {
            productRec(result, recursiveBound, vs);
        }
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < q; j++) {
                final int k = i * q + j;
                if (k >= 0 && k < n) {
                    result.addClause(vars[k].negate(f), us[i]);
                    result.addClause(vars[k].negate(f), vs[j]);
                }
            }
        }
    }

    private static void buildPure(final EncodingResult result, final Variable... vars) {
        for (int i = 0; i < vars.length; i++) {
            for (int j = i + 1; j < vars.length; j++) {
                result.addClause(vars[i].negate(result.factory()), vars[j].negate(result.factory()));
            }
        }
    }
}
