// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;

import java.util.function.Supplier;

/**
 * An event created when the {@link OptimizationFunction} found a better
 * bound.
 * @version 3.0.0
 * @since 3.0.0
 */
public class OptimizationFoundBetterBoundEvent implements LngEvent {

    private final Supplier<Model> model;

    /**
     * Creates a new event including a supplier to get the latest model.
     * @param model the latest model
     */
    public OptimizationFoundBetterBoundEvent(final Supplier<Model> model) {
        this.model = model;
    }

    /**
     * Returns the supplier for the latest model.
     * @return the supplier for the latest model
     */
    public Supplier<Model> getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "Event: Optimization Function found a better bound";
    }
}
