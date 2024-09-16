// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.CanonicalEnumeration;

/**
 * Canonical CNF generation via enumeration of falsifying assignments by a SAT
 * solver.
 * @version 3.0.0
 * @since 2.3.0
 */
public final class CanonicalCnfEnumeration extends CanonicalEnumeration {

    public CanonicalCnfEnumeration(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return compute(formula, handler, true);
    }
}
