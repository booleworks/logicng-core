package com.booleworks.logicng.handlers.events;

public class SddMinimizationStepEvent implements LngEvent {
    private final int newSize;

    public SddMinimizationStepEvent(final int newSize) {
        this.newSize = newSize;
    }

    public int getNewSize() {
        return newSize;
    }
}
