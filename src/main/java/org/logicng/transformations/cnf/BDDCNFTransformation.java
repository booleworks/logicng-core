// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import org.logicng.transformations.BDDNormalFormTransformation;

import java.util.Map;

/**
 * Transformation of a formula in CNF by converting it to a BDD.
 * @version 2.3.0
 * @since 1.4.0
 */
public final class BDDCNFTransformation extends BDDNormalFormTransformation {

    /**
     * Constructs a new BDD-based CNF transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     */
    public BDDCNFTransformation(final FormulaFactory f, final BDDKernel kernel) {
        super(true, f, kernel);
    }

    /**
     * Constructs a new BDD-based CNF transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     * @param cache  the cache to use for this transformation
     */
    public BDDCNFTransformation(final FormulaFactory f, final BDDKernel kernel, final Map<Formula, Formula> cache) {
        super(true, f, kernel, cache);
    }

    /**
     * Constructs a new BDD-based CNF transformation and constructs a new BDD kernel
     * for every formula application.
     * @param f the formula factory to generate new formulas
     */
    public BDDCNFTransformation(final FormulaFactory f) {
        super(true, f, null);
    }

    @Override
    public Formula apply(final Formula formula) {
        return compute(formula);
    }
}
