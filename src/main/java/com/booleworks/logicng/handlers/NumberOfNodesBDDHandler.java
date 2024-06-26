// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BDD_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.BDD_NEW_REF_ADDED;

import com.booleworks.logicng.handlers.events.LogicNGEvent;

/**
 * A BDD handler which cancels the build process after a given number of added
 * nodes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class NumberOfNodesBDDHandler implements ComputationHandler {

    private boolean aborted = false;
    private final int bound;
    private int count;

    /**
     * Constructs a new BDD handler with an upper bound for the number of added
     * nodes (inclusive).
     * @param bound the upper bound
     */
    public NumberOfNodesBDDHandler(final int bound) {
        if (bound < 0) {
            throw new IllegalArgumentException("The bound for added nodes must be equal or greater than 0.");
        }
        this.bound = bound;
    }

    @Override
    public boolean shouldResume(final LogicNGEvent event) {
        if (event == BDD_COMPUTATION_STARTED) {
            count = 0;
        } else if (event == BDD_NEW_REF_ADDED) {
            aborted = ++count >= bound;
        }
        return !aborted;
    }

    @Override
    public boolean isAborted() {
        return aborted;
    }
}
