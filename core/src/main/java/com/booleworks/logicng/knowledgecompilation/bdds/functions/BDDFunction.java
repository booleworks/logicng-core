// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.functions;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;

/**
 * A function on a BDD.
 * @param <T> the result type of the function
 * @version 3.0.0
 * @since 2.0.0
 */
public abstract class BDDFunction<T> {

    protected final FormulaFactory f;

    protected BDDFunction(final FormulaFactory f) {
        this.f = f;
    }

    /**
     * Applies this function on a given BDD.
     * @param bdd the BDD
     * @return the result of the application
     */
    public abstract T apply(BDD bdd);
}
