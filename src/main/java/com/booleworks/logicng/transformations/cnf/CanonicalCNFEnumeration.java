// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.transformations.CanonicalEnumeration;

/**
 * Canonical CNF generation via enumeration of falsifying assignments by a SAT
 * solver.
 * @version 3.0.0
 * @since 2.3.0
 */
public final class CanonicalCNFEnumeration extends CanonicalEnumeration {

    public CanonicalCNFEnumeration(final FormulaFactory f) {
        super(f);
    }

    @Override
    public Formula apply(final Formula formula) {
        return compute(formula, true);
    }
}
