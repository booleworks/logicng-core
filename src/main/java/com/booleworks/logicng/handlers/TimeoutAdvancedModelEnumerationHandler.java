// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

/**
 * A model enumeration handler which cancels the computation process after a given timeout.
 * @version 3.0.0
 * @since 3.0.0
 */
public class TimeoutAdvancedModelEnumerationHandler extends TimeoutModelEnumerationHandler implements AdvancedModelEnumerationHandler {

    /**
     * Constructs a new timeout handler with a given timeout and a timeout type.
     * The interpretation of the timeout depends on the timeout type:
     * <ul>
     *     <li>{@link TimerType#SINGLE_TIMEOUT}: The timeout is started when {@link Handler#started()} is called.
     *     Further calls to {@link Handler#started()} have no effect on the timeout. Thus, the timeout can only be started once.</li>
     *     <li>{@link TimerType#RESTARTING_TIMEOUT}: The timeout is restarted when {@link Handler#started()} is called.</li>
     *     <li>{@link TimerType#FIXED_END}: Timeout which is interpreted as fixed point in time (in milliseconds)
     *     at which the computation should be aborted. The method {@link Handler#started()} must still be called,
     *     but does not have an effect on the timeout.</li>
     * </ul>
     * Note that it might take a few milliseconds more until the computation is actually canceled,
     * since the handler depends on the next found model.
     * @param timeout the timeout in milliseconds, its meaning is defined by the timeout type
     * @param type    the type of the timer, must not be {@code null}
     */
    public TimeoutAdvancedModelEnumerationHandler(final long timeout, final TimerType type) {
        super(timeout, type);
    }

    /**
     * Constructs a new timeout handler with a given timeout and uses the timeout type {@link TimerType#SINGLE_TIMEOUT}.
     * Thus, the timeout is started when {@link Handler#started()} is called and further calls to {@link Handler#started()} have no effect on the timeout.
     * @param timeout the timeout in milliseconds
     */
    public TimeoutAdvancedModelEnumerationHandler(final long timeout) {
        super(timeout);
    }

    @Override
    public boolean foundModels(final int numberOfModel) {
        return !timeLimitExceeded();
    }

    @Override
    public boolean commit() {
        return !timeLimitExceeded();
    }

    @Override
    public boolean rollback() {
        return !timeLimitExceeded();
    }
}
