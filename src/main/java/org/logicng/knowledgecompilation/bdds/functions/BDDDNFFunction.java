// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.bdds.functions;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.knowledgecompilation.bdds.BDD;

/**
 * Creates a DNF from a BDD.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class BDDDNFFunction extends BDDNormalFormFunction {

    public BDDDNFFunction(final FormulaFactory f) {
        super(f);
    }

    @Override
    public Formula apply(final BDD bdd) {
        return compute(bdd, false);
    }
}
