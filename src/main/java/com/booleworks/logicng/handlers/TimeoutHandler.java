// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LNGEvent;

/**
 * A generic timeout handler.
 * @version 3.0.0
 * @since 3.0.0
 */
public class TimeoutHandler implements ComputationHandler {

    protected long timeout;
    protected final TimerType type;
    protected long designatedEnd;

    /**
     * Constructs a new timeout handler with a given timeout and a timeout
     * type. The interpretation of the timeout depends on the timeout type:
     * <ul>
     * <li>{@link TimerType#SINGLE_TIMEOUT}: The timeout is started when a
     * {@link ComputationStartedEvent} is received. Further
     * {@link ComputationStartedEvent}s have no effect on the timeout. Thus,
     * the timeout can only be started once.</li>
     * <li>{@link TimerType#RESTARTING_TIMEOUT}: The timeout is restarted when
     * a {@link ComputationStartedEvent} is received.</li>
     * <li>{@link TimerType#FIXED_END}: Timeout which is interpreted as fixed
     * point in time (in milliseconds) at which the computation should be
     * canceled.
     * </ul>
     * Note that it might take a few milliseconds more until the computation is
     * actually canceled, since the cancelation depends on the next call to
     * {@link #shouldResume}.
     * @param timeout the timeout in milliseconds, its meaning is defined by the
     *                timeout type
     * @param type    the type of the timer, must not be {@code null}
     */
    public TimeoutHandler(final long timeout, final TimerType type) {
        this.type = type;
        this.timeout = type == TimerType.FIXED_END ? 0 : timeout;
        designatedEnd = type == TimerType.FIXED_END ? timeout : 0;
    }

    /**
     * Constructs a new timeout handler with a given timeout and uses the
     * timeout type {@link TimerType#SINGLE_TIMEOUT}. Thus, the timeout is
     * started when the handler receives a {@link ComputationStartedEvent},
     * further events of this type have no effect on the timeout.
     * @param timeout the timeout in milliseconds
     */
    public TimeoutHandler(final long timeout) {
        this(timeout, TimerType.SINGLE_TIMEOUT);
    }

    @Override
    public boolean shouldResume(final LNGEvent event) {
        if (event instanceof ComputationStartedEvent) {
            if (type == TimerType.RESTARTING_TIMEOUT || designatedEnd == 0) {
                designatedEnd = System.currentTimeMillis() + timeout;
            }
        }
        return !timeLimitExceeded();
    }

    /**
     * Tests if the current time exceeds the timeout limit.
     * @return {@code true} if the current time exceeds the timeout limit,
     *         otherwise {@code false}
     */
    private boolean timeLimitExceeded() {
        return designatedEnd > 0 && System.currentTimeMillis() >= designatedEnd;
    }

    /**
     * Returns the designated end of this timeout handler as Linux timestamp.
     * @return the designated end
     */
    public long getDesignatedEnd() {
        return designatedEnd;
    }

    /**
     * A timeout type determines how a timeout is interpreted.
     */
    public enum TimerType {
        /**
         * Simple timeout which is started when a
         * {@link ComputationStartedEvent} is received.
         * <p>
         * Further {@link ComputationStartedEvent}s do not restart the timeout.
         */
        SINGLE_TIMEOUT,

        /**
         * Timeout which is restarted every time a
         * {@link ComputationStartedEvent} is received.
         */
        RESTARTING_TIMEOUT,

        /**
         * Timeout which is interpreted as fixed point in time (in milliseconds)
         * at which the computation should be canceled.
         */
        FIXED_END
    }
}
