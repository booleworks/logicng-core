// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.noncaching;

import org.logicng.formulas.And;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;

import java.util.LinkedHashSet;

public class LngNativeAnd extends LngNativeNAryOperator implements And {

    /**
     * Constructor.
     * @param operands the stream of operands
     * @param f        the factory which created this instance
     */
    LngNativeAnd(final LinkedHashSet<? extends Formula> operands, final NonCachingFormulaFactory f) {
        super(FType.AND, operands, f);
    }

    @Override
    public int hashCode() {
        return hashCode(31);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false; // the same formula factory would have produced a == object
        }
        if (other instanceof And) { // this is not really efficient... but should not be done anyway!
            return compareOperands(((And) other).operands());
        }
        return false;
    }
}
