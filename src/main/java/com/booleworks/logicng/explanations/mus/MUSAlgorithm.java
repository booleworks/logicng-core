// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.propositions.Proposition;

import java.util.List;

/**
 * Abstract super class for MUS computation algorithms.
 * @version 2.1.0
 * @since 1.1
 */
abstract class MUSAlgorithm {

    /**
     * Computes a MUS for the given propositions.
     * @param <T>          the type of the MUSes propositions
     * @param f            the formula factory
     * @param propositions the propositions
     * @param config       the MUS configuration
     * @return the MUS or null if the MUS computation was configured with a
     *         handler and this handler aborted the computation
     */
    public abstract <T extends Proposition> UNSATCore<T> computeMUS(final FormulaFactory f, final List<T> propositions,
                                                                    final MUSConfig config);
}
