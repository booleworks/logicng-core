// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.datastructures.Assignment;

import java.util.function.Supplier;

/**
 * Interface for an optimization handler.
 * @version 2.1.0
 * @since 2.1.0
 */
public interface OptimizationHandler extends Handler {

    /**
     * Returns a SAT handler which can be used to cancel internal SAT calls of
     * the optimization function.
     * @return a SAT handler
     */
    SATHandler satHandler();

    @Override
    default boolean aborted() {
        return satHandler() != null && satHandler().aborted();
    }

    /**
     * This method is called when the solver found a better bound for the
     * optimization.
     * @param currentResultProvider a provider for the current result, can be
     *                              used to examine the current result or to use
     *                              this result if the optimization should be
     *                              aborted
     * @return {@code true} if the optimization process should be continued,
     *         otherwise {@code false}
     */
    default boolean foundBetterBound(final Supplier<Assignment> currentResultProvider) {
        return true;
    }

    /**
     * Returns a SAT handler if the optimization handler is not {@code null}.
     * @param handler the optimization handler
     * @return The SAT handler if the optimization handler is not {@code null},
     *         otherwise {@code null}
     */
    static SATHandler satHandler(final OptimizationHandler handler) {
        return handler == null ? null : handler.satHandler();
    }
}
