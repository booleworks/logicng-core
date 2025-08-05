package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * Computation events for global SDD transformations.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddGlobalTransformationEvent implements LngEvent {
    /**
     * Event for starting a left rotation transformation.
     */
    public final static SddGlobalTransformationEvent START_LEFT_ROTATION =
            new SddGlobalTransformationEvent(true, "Start Left Rotation");

    /**
     * Event for starting a right rotation transformation.
     */
    public final static SddGlobalTransformationEvent START_RIGHT_ROTATION =
            new SddGlobalTransformationEvent(true, "Start Right Rotation");

    /**
     * Event for starting a swap children transformation.
     */
    public final static SddGlobalTransformationEvent START_SWAP = new SddGlobalTransformationEvent(true, "Start Swap");

    /**
     * Event for completing a left rotation transformation.
     */
    public final static SddGlobalTransformationEvent COMPLETED_LEFT_ROTATION =
            new SddGlobalTransformationEvent(false, "Completed Left Rotation");

    /**
     * Event for completing a right rotation transformation.
     */
    public final static SddGlobalTransformationEvent COMPLETED_RIGHT_ROTATION =
            new SddGlobalTransformationEvent(false, "Completed Right Rotation");

    /**
     * Event for completing a swap children transformation.
     */
    public final static SddGlobalTransformationEvent COMPLETED_SWAP =
            new SddGlobalTransformationEvent(false, "Completed Swap");

    private final String description;
    private final boolean isStart;

    /**
     * Construct new global transformation event.
     * @param isStart     whether the event is for starting or completing the
     *                    computation
     * @param description the description of the event
     */
    public SddGlobalTransformationEvent(final boolean isStart, final String description) {
        this.description = description;
        this.isStart = isStart;
    }

    /**
     * Returns the description of the event.
     * @return the description of the event
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns whether the event is for starting or completing the computation.
     * @return whether the event is for starting or completing the computation
     */
    public boolean isStart() {
        return isStart;
    }
}
