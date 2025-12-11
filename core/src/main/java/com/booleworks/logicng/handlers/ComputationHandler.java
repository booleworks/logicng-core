// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * Interface for a computation handler.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface ComputationHandler {
    /**
     * Processes the given event and returns {@code true} if the computation
     * should be resumed and {@code false} if it should be canceled.
     * @param event the event to handle, must not be {@code null}
     * @return whether the computation should be resumed or not
     */
    boolean shouldResume(LngEvent event);
}
