// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

/**
 * Interface for a handler for the advanced enumeration of models.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface AdvancedModelEnumerationHandler extends Handler {

    /**
     * Returns a SAT handler which can be used to cancel internal SAT calls of the model enumeration process.
     * @return a SAT handler
     */
    SATHandler satHandler();

    @Override
    default boolean aborted() {
        return satHandler() != null && satHandler().aborted();
    }

    /**
     * This method is called every time new models are found.
     * <p>
     * The found models are in an uncommitted state until they are confirmed by calling {@link #commit()}.
     * It is also possible to roll back the uncommitted models by calling {@link #rollback()}.
     * @param numberOfModels the number of found models
     * @return {@code true} if the computation should continue, otherwise {@code false}
     */
    boolean foundModels(int numberOfModels);

    /**
     * All founds models since the last commit call are confirmed and cannot be rolled back.
     * @return {@code true} if the computation should continue, otherwise {@code false}
     */
    boolean commit();

    /**
     * All found models since the last commit should be discarded.
     * @return {@code true} if the computation should continue, otherwise {@code false}
     */
    boolean rollback();
}
