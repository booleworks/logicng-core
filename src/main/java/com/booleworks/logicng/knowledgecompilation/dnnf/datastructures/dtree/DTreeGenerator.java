// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;

/**
 * A generator for a DTree.
 * @version 3.0.0
 * @since 2.0.0
 */
public interface DTreeGenerator {

    /**
     * Generates a DTree for the given CNF.
     * @param f   the formula factory
     * @param cnf the CNF
     * @return the DTree
     */
    default DTree generate(final FormulaFactory f, final Formula cnf) {
        return generate(f, cnf, NopHandler.get()).getResult();
    }

    /**
     * Generates a DTree for the given CNF.
     * @param f       the formula factory
     * @param cnf     the CNF
     * @param handler the computation handler
     * @return the DTree
     */
    LNGResult<DTree> generate(final FormulaFactory f, final Formula cnf, ComputationHandler handler);
}
