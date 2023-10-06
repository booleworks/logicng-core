// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import org.logicng.transformations.BDDNormalFormTransformation;

import java.util.Map;

/**
 * Transformation of a formula in DNF by converting it to a BDD.
 * @version 3.0.0
 * @since 2.3.0
 */
public final class BDDDNFTransformation extends BDDNormalFormTransformation {

    /**
     * Constructs a new BDD-based CNF transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     */
    public BDDDNFTransformation(final FormulaFactory f, final BDDKernel kernel) {
        super(false, f, kernel);
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
    public BDDDNFTransformation(final FormulaFactory f, final BDDKernel kernel, final Map<Formula, Formula> cache) {
        super(false, f, kernel, cache);
    }

    /**
     * Constructs a new BDD-based CNF transformation and constructs a new BDD kernel
     * for every formula application.
     * @param f the formula factory to generate new formulas
     */
    public BDDDNFTransformation(final FormulaFactory f) {
        super(false, f, null);
    }

    @Override
    public Formula apply(final Formula formula) {
        return compute(formula);
    }
}
