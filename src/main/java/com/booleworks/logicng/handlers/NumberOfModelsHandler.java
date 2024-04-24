// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

/**
 * A model enumeration handler that terminates the solving process after a given
 * number of models.
 * @version 3.0.0
 * @since 3.0.0
 */
public class NumberOfModelsHandler extends ComputationHandler implements ModelEnumerationHandler {

    private final int bound;
    private int countCommitted;
    private int countUncommitted;

    /**
     * Constructs a new model handler with an upper bound for the number of
     * models (inclusive).
     * @param bound the upper bound
     * @throws IllegalArgumentException if the number of models to generate is
     *                                  &lt;= 0
     */
    public NumberOfModelsHandler(final int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("You must generate at least 1 model.");
        }
        this.bound = bound;
    }

    @Override
    public void started() {
        super.started();
        countCommitted = 0;
        countUncommitted = 0;
    }

    @Override
    public SATHandler satHandler() {
        return null;
    }

    @Override
    public boolean foundModels(final int numberOfModels) {
        aborted = countUncommitted + countCommitted + numberOfModels > bound;
        if (!aborted) {
            countUncommitted += numberOfModels;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean commit() {
        countCommitted += countUncommitted;
        countUncommitted = 0;
        return true;
    }

    @Override
    public boolean rollback() {
        countUncommitted = 0;
        return true;
    }
}
