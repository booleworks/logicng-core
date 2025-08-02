package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;

/**
 * An event created when the {@link ModelEnumerationFunction} found a new set
 * of models.
 * <p>
 * Note that, due to the way the model enumeration function works, this event
 * will not be fired for all models.
 * @version 3.0.0
 * @since 3.0.0
 */
public class EnumerationFoundModelsEvent implements LngEvent {
    public static EnumerationFoundModelsEvent FOUND_ONE_MODEL = new EnumerationFoundModelsEvent(1);

    private final int numberOfModels;

    /**
     * Creates a new event with the given number of models.
     * @param numberOfModels the number of models which were found
     */
    public EnumerationFoundModelsEvent(final int numberOfModels) {
        this.numberOfModels = numberOfModels;
    }

    /**
     * Returns the number of models.
     * @return the number of models
     */
    public int getNumberOfModels() {
        return numberOfModels;
    }

    @Override
    public String toString() {
        return "Event: Model enumeration found " + numberOfModels + " new models";
    }
}
