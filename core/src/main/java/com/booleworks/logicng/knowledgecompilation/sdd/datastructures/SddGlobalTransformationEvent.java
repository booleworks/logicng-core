package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.handlers.events.LngEvent;

public class SddGlobalTransformationEvent implements LngEvent {
    public final static SddGlobalTransformationEvent START_LEFT_ROTATION =
            new SddGlobalTransformationEvent(true, "Start Left Rotation");
    public final static SddGlobalTransformationEvent START_RIGHT_ROTATION =
            new SddGlobalTransformationEvent(true, "Start Right Rotation");
    public final static SddGlobalTransformationEvent START_SWAP = new SddGlobalTransformationEvent(true, "Start Swap");
    public final static SddGlobalTransformationEvent COMPLETED_LEFT_ROTATION =
            new SddGlobalTransformationEvent(false, "Completed Left Rotation");
    public final static SddGlobalTransformationEvent COMPLETED_RIGHT_ROTATION =
            new SddGlobalTransformationEvent(false, "Completed Right Rotation");
    public final static SddGlobalTransformationEvent COMPLETED_SWAP =
            new SddGlobalTransformationEvent(false, "Completed Swap");

    private final String description;
    private final boolean isStart;

    public SddGlobalTransformationEvent(final boolean isStart, final String description) {
        this.description = description;
        this.isStart = isStart;
    }

    public String getDescription() {
        return description;
    }

    public boolean isStart() {
        return isStart;
    }
}
