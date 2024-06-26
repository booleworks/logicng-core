package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;

import java.util.function.Supplier;

/**
 * An event created when the {@link OptimizationFunction} found a better
 * bound.
 * @version 3.0.0
 * @since 3.0.0
 */
public class OptimizationFoundBetterBoundEvent implements LogicNGEvent {

    private final Supplier<Assignment> model;

    /**
     * Creates a new event including a supplier to get the latest model.
     * @param model the latest model
     */
    public OptimizationFoundBetterBoundEvent(final Supplier<Assignment> model) {
        this.model = model;
    }

    /**
     * Returns the supplier for the latest model.
     * @return the supplier for the latest model
     */
    public Supplier<Assignment> getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "Event: Optimization Function found a better bound";
    }
}
