// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.transformations.BddNormalFormTransformation;

import java.util.Map;

/**
 * Transformation of a formula in CNF by converting it to a BDD.
 * @version 2.3.0
 * @since 1.4.0
 */
public final class BddCnfTransformation extends BddNormalFormTransformation {

    /**
     * Constructs a new BDD-based CNF transformation with an optional BDD
     * kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations,
     * <b>but</b> the number of different variables in all applied formulas
     * <b>must not exceed</b> the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     */
    public BddCnfTransformation(final FormulaFactory f, final BddKernel kernel) {
        super(f, true, kernel);
    }

    /**
     * Constructs a new BDD-based CNF transformation with an optional BDD
     * kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations,
     * <b>but</b> the number of different variables in all applied formulas
     * <b>must not exceed</b> the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     * @param cache  the cache to use for this transformation
     */
    public BddCnfTransformation(final FormulaFactory f, final BddKernel kernel, final Map<Formula, Formula> cache) {
        super(f, true, kernel, cache);
    }

    /**
     * Constructs a new BDD-based CNF transformation and constructs a new BDD
     * kernel for every formula application.
     * @param f the formula factory to generate new formulas
     */
    public BddCnfTransformation(final FormulaFactory f) {
        super(f, true, null);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return compute(formula, handler);
    }
}
