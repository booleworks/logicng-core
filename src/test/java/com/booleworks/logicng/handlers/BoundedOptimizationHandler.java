// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.datastructures.Assignment;

import java.util.function.Supplier;

/**
 * Bounded optimization handler for testing purposes.
 * <p>
 * The handler aborts the optimization if a certain number of starts or a certain number of SAT handler starts is reached.
 * @version 2.1.0
 * @since 2.1.0
 */
public class BoundedOptimizationHandler implements OptimizationHandler {
    private final SATHandler satHandler;
    private final int startsLimit;
    private int numStarts;
    private boolean aborted;

    /**
     * Constructs a new instance with the given starts limits.
     * @param satHandlerStartsLimit the number of starts limit for the SAT handler, if -1 then no limit is set
     * @param startsLimit           the number of starts limit, if -1 then no limit is set
     */
    public BoundedOptimizationHandler(final int satHandlerStartsLimit, final int startsLimit) {
        satHandler = new BoundedSatHandler(satHandlerStartsLimit);
        this.startsLimit = startsLimit;
        numStarts = 0;
    }

    @Override
    public boolean aborted() {
        return satHandler.aborted() || aborted;
    }

    @Override
    public void started() {
        aborted = startsLimit != -1 && ++numStarts >= startsLimit;
    }

    @Override
    public SATHandler satHandler() {
        return satHandler;
    }

    @Override
    public boolean foundBetterBound(final Supplier<Assignment> currentResultProvider) {
        return !aborted;
    }
}
