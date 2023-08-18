// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.handlers;

/**
 * Bounded SAT handler for testing purposes.
 * <p>
 * The handler aborts the computation if a certain number of starts is reached.
 * @version 2.1.0
 * @since 2.1.0
 */
public class BoundedSatHandler implements SATHandler {
    private final int startsLimit;
    private int numStarts;
    private boolean aborted;

    /**
     * Constructs a new instance with the given starts limit.
     * @param startsLimit the number of starts limit, if -1 then no limit is set
     */
    public BoundedSatHandler(final int startsLimit) {
        this.startsLimit = startsLimit;
        numStarts = 0;
    }

    @Override
    public boolean aborted() {
        return aborted;
    }

    @Override
    public void started() {
        aborted = startsLimit != -1 && ++numStarts >= startsLimit;
    }

    @Override
    public boolean detectedConflict() {
        return !aborted;
    }
}
