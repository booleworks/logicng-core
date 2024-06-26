// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LogicNGEvent;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.handlers.events.SimpleEvent;

public final class EnumerationCollectorTestHandler implements ComputationHandler {

    private int foundModels;
    private int commitCalls;
    private int rollbackCalls;

    @Override
    public boolean shouldResume(final LogicNGEvent event) {
        if (event == ComputationStartedEvent.MODEL_ENUMERATION_STARTED) {
            foundModels = 0;
        } else if (event instanceof EnumerationFoundModelsEvent) {
            foundModels += ((EnumerationFoundModelsEvent) event).getNumberOfModels();
        } else if (event == SimpleEvent.MODEL_ENUMERATION_COMMIT) {
            ++commitCalls;
        } else if (event == SimpleEvent.MODEL_ENUMERATION_ROLLBACK) {
            ++rollbackCalls;
        }
        return true;
    }

    @Override
    public boolean isAborted() {
        return false;
    }

    public int getFoundModels() {
        return foundModels;
    }

    public int getCommitCalls() {
        return commitCalls;
    }

    public int getRollbackCalls() {
        return rollbackCalls;
    }
}
