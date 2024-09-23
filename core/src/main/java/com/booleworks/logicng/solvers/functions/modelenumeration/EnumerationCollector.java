// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.SatSolver;

import java.util.List;

/**
 * An interface for enumeration collectors.
 * <p>
 * An enumeration collector gathers the found models given by
 * {@link #addModel(LngBooleanVector, SatSolver, LngIntVector, ComputationHandler)}.
 * Added Models added can potentially be discarded later via
 * {@link #rollback(ComputationHandler)}. To prevent models from being
 * rolled back one can call {@link #commit(ComputationHandler)}. With
 * {@link #getResult()} the result, the models committed models, can be
 * retrieved.
 * @param <RESULT> The result type of the model enumeration function. Can be
 *                 e.g. a model count, a list of models, or a BDD.
 * @version 3.0.0
 * @since 2.5.0
 */
public interface EnumerationCollector<RESULT> {

    /**
     * Add model to the enumeration collector.
     * @param modelFromSolver    the model from the solver
     * @param solver             the solver
     * @param relevantAllIndices the relevant indices
     * @param handler            the model enumeration handler
     * @return an event if the handler canceled the computation,
     *         otherwise {@code null}
     */
    LngEvent addModel(LngBooleanVector modelFromSolver, SatSolver solver, LngIntVector relevantAllIndices,
                      ComputationHandler handler);

    /**
     * All founds models since the last commit call are confirmed and cannot be
     * rolled back.
     * <p>
     * Calls the {@code commit()} routine of {@code handler}.
     * @param handler the computation handler
     * @return an event if the handler canceled the computation,
     *         otherwise {@code null}
     */
    LngEvent commit(ComputationHandler handler);

    /**
     * All found models since the last commit should be discarded.
     * <p>
     * The rollback should <b>always</b> be performed, even if the handler
     * cancels the computation.
     * @param handler the computation handler
     * @return an event if the handler canceled the computation,
     *         otherwise {@code null}
     */
    LngEvent rollback(ComputationHandler handler);

    /**
     * All found models since the last commit will be discarded and returned.
     * <p>
     * Calls the {@code rollback} routine of {@code handler}.
     * @param solver  solver used for the enumeration
     * @param handler the computation handler
     * @return the LNG result with a list of all discarded models or a canceled
     *         result if the rollback was canceled by the handler
     */
    LngResult<List<Model>> rollbackAndReturnModels(final SatSolver solver, ComputationHandler handler);

    /**
     * Returns the currently committed state of the collector.
     * @return the currently committed state of the collector
     */
    RESULT getResult();
}
