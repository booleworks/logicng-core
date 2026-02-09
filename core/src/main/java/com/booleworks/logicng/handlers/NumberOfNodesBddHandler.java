// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BDD_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.BDD_NEW_REF_ADDED;

import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * A BDD handler which cancels the build process after a given number of added
 * nodes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class NumberOfNodesBddHandler implements ComputationHandler {

    protected boolean canceled = false;
    protected final int bound;
    protected int count;

    /**
     * Constructs a new BDD handler with an upper bound for the number of added
     * nodes (inclusive).
     * @param bound the upper bound
     */
    public NumberOfNodesBddHandler(final int bound) {
        if (bound < 0) {
            throw new IllegalArgumentException("The bound for added nodes must be equal or greater than 0.");
        }
        this.bound = bound;
    }

    @Override
    public boolean shouldResume(final LngEvent event) {
        if (event == BDD_COMPUTATION_STARTED) {
            count = 0;
        } else if (event == BDD_NEW_REF_ADDED) {
            canceled = ++count >= bound;
        }
        return !canceled;
    }
}
