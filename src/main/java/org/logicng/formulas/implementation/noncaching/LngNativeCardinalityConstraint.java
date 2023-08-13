// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.implementation.noncaching;

import org.logicng.formulas.CType;
import org.logicng.formulas.CardinalityConstraint;
import org.logicng.formulas.Literal;

import java.util.Collections;
import java.util.List;

public class LngNativeCardinalityConstraint extends LngNativePBConstraint implements CardinalityConstraint {

    /**
     * Constructs a new cardinality constraint.
     * @param literals   the literals
     * @param comparator the comparator
     * @param rhs        the right-hand side, has to follow the restrictions in the class description
     * @param f          the formula factory
     * @throws IllegalArgumentException if the number of literals and coefficients do not correspond
     */
    LngNativeCardinalityConstraint(final List<? extends Literal> literals, final CType comparator, final int rhs, final NonCachingFormulaFactory f) {
        super(literals, Collections.nCopies(literals.size(), 1), comparator, rhs, f);
    }
}
