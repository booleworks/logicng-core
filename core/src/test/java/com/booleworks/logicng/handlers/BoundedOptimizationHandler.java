// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * Bounded optimization handler for testing purposes.
 * <p>
 * The handler cancels the optimization if a certain number of starts or a
 * certain number of SAT handler starts is reached.
 * @version 2.1.0
 * @since 2.1.0
 */
public class BoundedOptimizationHandler implements ComputationHandler {
    private final int startsLimit;
    private final int satStartsLimit;
    private int numStarts;
    private int numSatStarts;
    private boolean canceled;

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
    public boolean shouldResume(final LngEvent event) {
        if (event instanceof ComputationStartedEvent) {
            if (event == ComputationStartedEvent.SAT_CALL_STARTED) {
                canceled |= satStartsLimit != -1 && ++numSatStarts >= satStartsLimit;
            } else {
                canceled |= startsLimit != -1 && ++numStarts >= startsLimit;
            }
        }
        return !canceled;
    }
}
