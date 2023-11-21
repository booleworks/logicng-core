// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.OptimizationHandler;
import com.booleworks.logicng.transformations.AbortableFormulaTransformation;

/**
 * An implementation of the Quine-McCluskey algorithm for minimizing canonical DNFs.  This implementation uses
 * the {@link AdvancedSimplifier} to compute the minimization. This is far more efficient than performing
 * manual term table manipulations but still has its limits.  An optional handler can be provided to abort
 * long-running computations.
 * @version 3.0.0
 * @since 1.4.0
 */
public class QuineMcCluskeySimplifier extends AbortableFormulaTransformation<OptimizationHandler> {

    /**
     * Constructor.
     * @param f the formula factory to generate new formulas
     **/
    protected QuineMcCluskeySimplifier(final FormulaFactory f) {
        super(f, null);
    }

    /**
     * Constructor.
     * @param f       the formula factory to generate new formulas
     * @param handler the optimization handler to abort Quine-McCluskey
     **/
    protected QuineMcCluskeySimplifier(final FormulaFactory f, final OptimizationHandler handler) {
        super(f, handler);
    }

    @Override
    public Formula apply(final Formula formula) {
        final var qmcConfig = AdvancedSimplifierConfig.builder()
                .factorOut(false)
                .restrictBackbone(false)
                .simplifyNegations(false)
                .useRatingFunction(false)
                .build();
        final var advancedSimplifier = new AdvancedSimplifier(f, qmcConfig, handler);
        return formula.transform(advancedSimplifier);
    }
}
