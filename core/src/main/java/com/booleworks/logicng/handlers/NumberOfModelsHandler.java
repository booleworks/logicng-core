// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SimpleEvent;

/**
 * A model enumeration handler that terminates the solving process after a given
 * number of models.
 * <p>
 * <i>Note that it is still possible that the computation yields <b>more</b>
 * models than the given bound</i>, since there are cases where several models
 * are added at once.
 * @version 3.0.0
 * @since 3.0.0
 */
public class NumberOfModelsHandler implements ComputationHandler {

    private final int bound;
    private int countCommitted;
    private int countUncommitted;

    /**
     * Constructs a new model handler with an upper bound for the number of
     * models (inclusive).
     * <p>
     * <i>Note that it is still possible that the computation yields <b>more</b>
     * models than the given bound</i>, since there are cases where several
     * models are added at once.
     * @param bound the upper bound
     * @throws IllegalArgumentException if the number of models to generate is
     *                                  &lt;= 0
     */
    public NumberOfModelsHandler(final int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("You must generate at least 1 model.");
        }
        this.bound = bound;
    }

    @Override
    public boolean shouldResume(final LngEvent event) {
        if (event == ComputationStartedEvent.MODEL_ENUMERATION_STARTED) {
            countCommitted = 0;
            countUncommitted = 0;
        } else if (event instanceof EnumerationFoundModelsEvent) {
            final int numberOfModels = ((EnumerationFoundModelsEvent) event).getNumberOfModels();
            countUncommitted += numberOfModels;
        } else if (event == SimpleEvent.MODEL_ENUMERATION_COMMIT) {
            countCommitted += countUncommitted;
            countUncommitted = 0;
        } else if (event == SimpleEvent.MODEL_ENUMERATION_ROLLBACK) {
            countUncommitted = 0;
        }
        return countUncommitted + countCommitted < bound;
    }
}
