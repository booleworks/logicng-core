package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LNGEvent;

/**
 * A computation handler which never cancels the computation.
 * <p>
 * {@link #get()} returns the one and only instance of this class.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class NopHandler implements ComputationHandler {

    private static final NopHandler INSTANCE = new NopHandler();

    private NopHandler() {
    }

    /**
     * Returns the singleton instance of this class.
     * @return the singleton instance of this class
     */
    public static NopHandler get() {
        return INSTANCE;
    }

    @Override
    public boolean shouldResume(final LNGEvent event) {
        return true;
    }
}
