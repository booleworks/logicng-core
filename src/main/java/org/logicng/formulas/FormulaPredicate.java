// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

/**
 * A predicate on a formula.
 * @version 2.0.0
 * @since 1.0
 */
@FunctionalInterface
public interface FormulaPredicate {

    /**
     * Tests the predicate on a given formula.
     * @param formula the input formula
     * @return {@code true} if the formula holds, {@code false} otherwise
     */
    boolean test(Formula formula);
}
