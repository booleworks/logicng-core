// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.propositions.Proposition;

import java.util.List;

/**
 * Abstract super class for MUS computation algorithms.
 * @version 3.0.0
 * @since 1.1
 */
abstract class MusAlgorithm {

    /**
     * Computes a MUS for the given propositions.
     * @param <T>          the type of the MUSes propositions
     * @param f            the formula factory
     * @param propositions the propositions
     * @param config       the MUS configuration
     * @param handler      the computation handler
     * @return an LNG result containing the MUS (unless the handler canceled
     * the computation)
     * @throws IllegalArgumentException if the set of propositions is
     *                                  satisfiable
     */
    public abstract <T extends Proposition> LngResult<UnsatCore<T>> computeMus(
            final FormulaFactory f, final List<T> propositions,
            final MusConfig config, final ComputationHandler handler);

    /**
     * Computes a MUS for the given propositions.
     * @param <T>          the type of the MUSes propositions
     * @param f            the formula factory
     * @param propositions the propositions
     * @param config       the MUS configuration
     * @return the MUS
     * @throws IllegalArgumentException if the set of propositions is
     *                                  satisfiable
     */
    public <T extends Proposition> UnsatCore<T> computeMus(
            final FormulaFactory f, final List<T> propositions, final MusConfig config) {
        return computeMus(f, propositions, config, NopHandler.get()).getResult();
    }
}
