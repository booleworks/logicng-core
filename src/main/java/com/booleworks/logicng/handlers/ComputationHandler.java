package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LogicNGEvent;

/**
 * Interface for a computation handler.
 * @since 3.0.0
 * @version 3.0.0
 */
public interface ComputationHandler {
    /**
     * Processes the given event and returns {@code true} if the computation
     * should be resumed and {@code false} if it should be aborted.
     * @param event the event to handle, must not be {@code null}
     * @return whether the computation should be resumed or not
     */
    boolean shouldResume(LogicNGEvent event);

    /**
     * @deprecated should be removed
     */
    @Deprecated
    boolean isAborted();
}
