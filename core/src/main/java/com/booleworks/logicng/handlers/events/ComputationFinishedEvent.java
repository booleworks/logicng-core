// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.handlers.ComputationHandler;

/**
 * A class for {@link LngEvent}s which indicate the end of a computation.
 * By convention, these events are instantiated as {@code static final} fields
 * s.t. they can be checked in {@link ComputationHandler handlers} by using
 * referential equality.
 * <p>
 * The {@link #computationType} should only be used for debugging purposes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class ComputationFinishedEvent implements LngEvent {

    public static final ComputationFinishedEvent SAT_CALL_FINISHED = new ComputationFinishedEvent("SAT Call");
    public static final ComputationFinishedEvent MAX_SAT_CALL_FINISHED = new ComputationFinishedEvent("MaxSAT Call");

    private final String computationType;

    /**
     * Creates a new event for the given computation type.
     * @param computationType the computation type
     */

    public ComputationFinishedEvent(final String computationType) {
        this.computationType = computationType;
    }

    /**
     * Returns the computation type.
     * @return the computation type
     */
    public String getComputationType() {
        return computationType;
    }

    @Override
    public String toString() {
        return "Event: Finished " + computationType;
    }
}
