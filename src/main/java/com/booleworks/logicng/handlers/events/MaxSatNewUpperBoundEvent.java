package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.solvers.MaxSATSolver;

/**
 * An event created when a {@link MaxSATSolver} found a new upper bound.
 * @version 3.0.0
 * @since 3.0.0
 */
public class MaxSatNewUpperBoundEvent implements LogicNGEvent {
    private final int bound;

    /**
     * Creates a new event with the given upper bound.
     * @param bound the new upper bound
     */

    public MaxSatNewUpperBoundEvent(final int bound) {
        this.bound = bound;
    }

    /**
     * Returns the new upper bound.
     * @return the new upper bound
     */
    public int getBound() {
        return bound;
    }

    @Override
    public String toString() {
        return "Event: New Max SAT upper bound: " + bound;
    }
}
