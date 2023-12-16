// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;

/**
 * A generator for a DTree.
 * @version 2.0.0
 * @since 2.0.0
 */
public interface DTreeGenerator {

    /**
     * Generates a DTree for the given CNF.
     * @param f   the formula factory
     * @param cnf the CNF
     * @return the DTree
     */
    DTree generate(final FormulaFactory f, final Formula cnf);
}
