// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LNGEvent;

/**
 * Bounded optimization handler for testing purposes.
 * <p>
 * The handler aborts the optimization if a certain number of starts or a
 * certain number of SAT handler starts is reached.
 * @version 2.1.0
 * @since 2.1.0
 */
public class BoundedOptimizationHandler implements ComputationHandler {
    private final int startsLimit;
    private final int satStartsLimit;
    private int numStarts;
    private int numSatStarts;
    private boolean aborted;

    /**
     * Constructs a new instance with the given starts limits.
     * @param satStartsLimit the number of starts limit for the SAT
     *                       solver, if -1 then no limit is set
     * @param startsLimit    the number of starts limit, if -1 then no
     *                       limit is set
     */
    public BoundedOptimizationHandler(final int satStartsLimit, final int startsLimit) {
        this.satStartsLimit = satStartsLimit;
        this.startsLimit = startsLimit;
        numStarts = 0;
    }

    @Override
    public boolean shouldResume(final LNGEvent event) {
        if (event instanceof ComputationStartedEvent) {
            if (event == ComputationStartedEvent.SAT_CALL_STARTED) {
                aborted |= satStartsLimit != -1 && ++numSatStarts >= satStartsLimit;
            } else {
                aborted |= startsLimit != -1 && ++numStarts >= startsLimit;
            }
        }
        return !aborted;
    }
}
