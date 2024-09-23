package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LngEvent;

public class CallLimitComputationHandler implements ComputationHandler {
    private final int n;
    private int count;

    public CallLimitComputationHandler(final int n) {
        this.n = n;
        count = 0;
    }

    @Override
    public boolean shouldResume(final LngEvent event) {
        return count++ < n;
    }
}
