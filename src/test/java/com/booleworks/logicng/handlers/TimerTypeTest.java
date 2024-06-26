// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import org.junit.jupiter.api.Test;

public class TimerTypeTest {

    public static final ComputationStartedEvent COMPUTATION_STARTED = new ComputationStartedEvent("dummy");

    @Test
    public void testSingleTimeout() throws InterruptedException {
        final TimeoutHandler handler = handler(100L, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isFalse();
        Thread.sleep(200L);
        assertThat(handler.isAborted()).isTrue();
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isTrue();
    }

    @Test
    public void testRestartingTimeout() throws InterruptedException {
        final TimeoutHandler handler = handler(100L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT);
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isFalse();
        Thread.sleep(200L);
        assertThat(handler.isAborted()).isTrue();
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isFalse();
        Thread.sleep(200L);
        assertThat(handler.isAborted()).isTrue();
    }

    @Test
    public void testFixedEnd() throws InterruptedException {
        final TimeoutHandler handler = fixedEndHandler(100L);
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isFalse();
        Thread.sleep(200L);
        assertThat(handler.isAborted()).isTrue();
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isTrue();
    }

    @Test
    public void testFixedEndWithDelayedStartedCall() throws InterruptedException {
        final TimeoutHandler handler = fixedEndHandler(100L);
        Thread.sleep(200L);
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isTrue();
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isTrue();
    }

    @Test
    public void testFixedEndWithZeroTime() {
        final TimeoutHandler handler = fixedEndHandler(0L);
        assertThat(handler.isAborted()).isTrue();
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isTrue();
    }

    @Test
    public void testFixedEndWithPastPointInTime() {
        final TimeoutHandler handler = fixedEndHandler(-100L);
        assertThat(handler.isAborted()).isTrue();
        handler.shouldResume(COMPUTATION_STARTED);
        assertThat(handler.isAborted()).isTrue();
    }

    private static TimeoutHandler handler(final long timeout, final TimeoutHandler.TimerType type) {
        return new TimeoutHandler(timeout, type);
    }

    private static TimeoutHandler fixedEndHandler(final long delta) {
        return new TimeoutHandler(System.currentTimeMillis() + delta, TimeoutHandler.TimerType.FIXED_END);
    }
}
