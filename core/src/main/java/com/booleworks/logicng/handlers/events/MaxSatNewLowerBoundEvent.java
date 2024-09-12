package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.solvers.MaxSATSolver;

/**
 * An event created when a {@link MaxSATSolver} found a new lower bound.
 * @version 3.0.0
 * @since 3.0.0
 */
public class MaxSatNewLowerBoundEvent implements LNGEvent {
    private final int bound;

    /**
     * Creates a new event with the given lower bound.
     * @param bound the new lower bound
     */
    public MaxSatNewLowerBoundEvent(final int bound) {
        this.bound = bound;
    }

    /**
     * Returns the new lower bound.
     * @return the new lower bound
     */
    public int getBound() {
        return bound;
    }

    @Override
    public String toString() {
        return "Event: New Max SAT lower bound: " + bound;
    }
}
