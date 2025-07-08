package com.booleworks.logicng.handlers.events;

public class SddMinimizationEvent implements LngEvent {
    private final long newSize;
    private final boolean isFragmentStep;

    public SddMinimizationEvent(final long newSize, final boolean isFragmentStep) {
        this.newSize = newSize;
        this.isFragmentStep = isFragmentStep;
    }

    public long getNewSize() {
        return newSize;
    }

    public boolean isFragmentStep() {
        return isFragmentStep;
    }
}
