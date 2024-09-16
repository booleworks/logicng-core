// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * Bounded SAT handler for testing purposes.
 * <p>
 * The handler cancels the computation if a certain number of starts is reached.
 * @version 3.0.0
 * @since 2.1.0
 */
public class BoundedSatHandler implements ComputationHandler {
    private final int startsLimit;
    private int numStarts;
    private boolean canceled;

    /**
     * Constructs a new instance with the given starts limit.
     * @param startsLimit the number of starts limit, if -1 then no limit is set
     */
    public BoundedSatHandler(final int startsLimit) {
        this.startsLimit = startsLimit;
        numStarts = 0;
    }

    @Override
    public boolean shouldResume(final LngEvent event) {
        if (event == ComputationStartedEvent.SAT_CALL_STARTED) {
            canceled = startsLimit != -1 && ++numStarts >= startsLimit;
        }
        return !canceled;
    }
}
