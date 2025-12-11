// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

/**
 * An interface for a function which operates on an SDD node.
 * <p>
 * {@link RESULT} is the result typed returned by an implementation of an SDD
 * function.  The interface provides overloaded functions
 * {@link SddFunction#execute(SddNode, ComputationHandler) execute(SddNode, ...)}
 * that execute the function on a given SDD node.  One variant additional takes
 * a {@link ComputationHandler} that can control and abort the computation.
 * This variant returns a {@link LngResult<RESULT>} that carries the cancel
 * cause if the computation was aborted.  The other variant does not take
 * additional arguments and returns {@link RESULT}.
 * <p>
 * The most common way to run SDD functions is by using the methods
 * {@link SddNode#execute(SddFunction, ComputationHandler) SddNode.execute()}:
 * <pre>{@code
 * SddFunction<R> myFunc = new MySddFunction();
 * R result1 = sddNode1.execute(myFunc);
 * LngResult<R> result2 = sddNode2.execute(myFunc, myHandler);
 * }</pre>
 * <p>
 * Implementation Note: Each implementation of this interface must be
 * implemented in such a way that a canceled (or partial) {@code LngResult} is
 * returned if and only if the computation was aborted by {@code handler}
 * provided by the user.
 * @param <RESULT> the result type of the function
 * @version 3.0.0
 * @since 3.0.0
 */
public interface SddFunction<RESULT> {
    /**
     * Applies this function to the given SDD node.
     * <p>
     * The function will return a canceled or partial result if and only if the
     * computation was aborted by {@code handler}.
     * @param node    the SDD node to which the function is applied
     * @param handler the computation handler
     * @return the (potentially canceled) result of the function application
     * @see SddNode#execute(SddFunction, ComputationHandler)
     */
    LngResult<RESULT> execute(final SddNode node, final ComputationHandler handler);

    /**
     * Applies this function to the given SDD node.
     * <p>
     * This function is also aliased as
     * {@link SddNode#execute(SddFunction)}.
     * @param node the SDD node to which the function is applied
     * @return the (potentially canceled) result of the function application
     * @see SddNode#execute(SddFunction)
     */
    default RESULT execute(final SddNode node) {
        return execute(node, NopHandler.get()).getResult();
    }
}
