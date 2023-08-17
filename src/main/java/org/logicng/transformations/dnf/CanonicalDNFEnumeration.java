// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.transformations.CanonicalEnumeration;

/**
 * Canonical DNF generation via enumeration of models by a SAT solver.
 * @version 3.0.0
 * @since 1.0
 */
public final class CanonicalDNFEnumeration extends CanonicalEnumeration {

    public CanonicalDNFEnumeration(final FormulaFactory f) {
        super(f);
    }

    @Override
    public Formula apply(final Formula formula) {
        return compute(formula, false);
    }
}
