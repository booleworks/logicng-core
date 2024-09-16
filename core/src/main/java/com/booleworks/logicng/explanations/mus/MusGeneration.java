// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.propositions.Proposition;

import java.util.List;

/**
 * Computes a minimal unsatisfiable subset (MUS) of a given formula with
 * different algorithms.
 * @version 2.0.0
 * @since 1.1
 */
public final class MusGeneration {

    private final DeletionBasedMus deletion;
    private final PlainInsertionBasedMus insertion;

    /**
     * Constructs a new MUS generator.
     */
    public MusGeneration() {
        deletion = new DeletionBasedMus();
        insertion = new PlainInsertionBasedMus();
    }

    /**
     * Computes a MUS for the given propositions with the default algorithm and
     * the MUS configuration from the formula factory.
     * @param <T>          the type of the MUSes propositions
     * @param f            the formula factory
     * @param propositions the propositions
     * @return the MUS
     */
    public <T extends Proposition> UnsatCore<T> computeMus(final FormulaFactory f, final List<T> propositions) {
        return computeMus(f, propositions, (MusConfig) f.configurationFor(ConfigurationType.MUS));
    }

    /**
     * Computes a MUS for the given propositions and the given configuration of
     * the MUS generation.
     * @param <T>          the type of the MUSes propositions
     * @param f            the formula factory
     * @param propositions the propositions
     * @param config       the MUS configuration
     * @return the MUS
     */
    public <T extends Proposition> UnsatCore<T> computeMus(final FormulaFactory f, final List<T> propositions,
                                                           final MusConfig config) {
        return computeMus(f, propositions, config, NopHandler.get()).getResult();
    }

    /**
     * Computes a MUS for the given propositions and the given configuration of
     * the MUS generation.
     * @param <T>          the type of the MUSes propositions
     * @param f            the formula factory
     * @param propositions the propositions
     * @param config       the MUS configuration
     * @param handler      the computation handler
     * @return the MUS
     */
    public <T extends Proposition> LngResult<UnsatCore<T>> computeMus(
            final FormulaFactory f, final List<T> propositions,
            final MusConfig config, final ComputationHandler handler) {
        if (propositions.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate a MUS for an empty list of propositions");
        }
        switch (config.algorithm) {
            case PLAIN_INSERTION:
                return insertion.computeMus(f, propositions, config, handler);
            case DELETION:
                return deletion.computeMus(f, propositions, config, handler);
            default:
                throw new IllegalStateException("Unknown MUS algorithm: " + config.algorithm);
        }
    }
}
