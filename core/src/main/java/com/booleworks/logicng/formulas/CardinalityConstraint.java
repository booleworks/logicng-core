// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

/**
 * A cardinality constraint of the form {@code l_1 + ... + l_n R k} where
 * {@code R} is one of {@code =, >, >=, <, <=} and with the following
 * restrictions:
 * <ul>
 * <li>The right-hand side {@code k} is greater or equal 0 for
 * {@code =, >=, <=}</li>
 * <li>The right-hand side {@code k} is greater or equal -1 for {@code >}</li>
 * <li>The right-hand side {@code k} is greater or equal 1 for {@code <}</li>
 * </ul>
 * @version 3.0.0
 * @since 2.0.0
 */
public interface CardinalityConstraint extends PbConstraint {

    @Override
    default boolean isCc() {
        return true;
    }

    @Override
    default boolean isAmo() {
        return comparator() == CType.LE && getRhs() == 1 || comparator() == CType.LT && getRhs() == 2;
    }

    @Override
    default boolean isExo() {
        return comparator() == CType.EQ && getRhs() == 1;
    }
}
