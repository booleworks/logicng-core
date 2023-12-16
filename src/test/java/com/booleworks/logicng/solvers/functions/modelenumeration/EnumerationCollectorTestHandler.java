// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.handlers.SATHandler;

public final class EnumerationCollectorTestHandler implements ModelEnumerationHandler {

    private int foundModels;
    private int commitCalls;
    private int rollbackCalls;

    @Override
    public void started() {
        ModelEnumerationHandler.super.started();
        foundModels = 0;
    }

    @Override
    public SATHandler satHandler() {
        return null;
    }

    @Override
    public boolean foundModels(final int numberOfModels) {
        foundModels += numberOfModels;
        return true;
    }

    @Override
    public boolean commit() {
        ++commitCalls;
        return true;
    }

    @Override
    public boolean rollback() {
        ++rollbackCalls;
        return true;
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
