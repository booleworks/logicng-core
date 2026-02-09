// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;

/**
 * Creates a CNF from a BDD.
 * @version 3.0.0
 * @since 2.0.0
 */
public class BddCnfFunction extends BddNormalFormFunction {

    /**
     * Constructs a new instance of a function that converts BDDs to CNF.
     * @param f the formula factory
     */
    public BddCnfFunction(final FormulaFactory f) {
        super(f);
    }

    @Override
    public Formula apply(final Bdd bdd) {
        return compute(bdd, true);
    }
}
