// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.FIXED_END;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.SINGLE_TIMEOUT;
import static com.booleworks.logicng.handlers.events.ComputationFinishedEvent.SAT_CALL_FINISHED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SAT_CALL_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SAT_CONFLICT_DETECTED;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class TimeoutSATHandlerTest {

    private FormulaFactory f;
    private PigeonHoleGenerator pg;
    private List<SatSolver> solvers;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        pg = new PigeonHoleGenerator(f);
        solvers = SolverTestSet.solverTestSet(Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD), f);
    }

    @Test
    public void testDetectedConflict() throws InterruptedException {
        final TimeoutHandler handler = new TimeoutHandler(100, SINGLE_TIMEOUT);
        handler.shouldResume(SAT_CALL_STARTED);
        assertThat(handler.shouldResume(SAT_CONFLICT_DETECTED)).isTrue();
        Thread.sleep(200);
        assertThat(handler.shouldResume(SAT_CONFLICT_DETECTED)).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        for (final SatSolver solver : solvers) {
            solver.add(f.parse("(x => y) & (~x => y) & (y => z) & (z => ~y)"));
            final TimeoutHandler handler = Mockito.mock(TimeoutHandler.class);
            when(handler.shouldResume(any())).thenReturn(true);
            solver.satCall().handler(handler).solve().close();
            verify(handler, times(1)).shouldResume(eq(SAT_CALL_STARTED));
            verify(handler, atLeast(1)).shouldResume(eq(SAT_CONFLICT_DETECTED));
            verify(handler, times(1)).shouldResume(eq(SAT_CALL_FINISHED));
        }
    }

    @Test
    public void testThatDetectedConflictIsHandledProperly() {
        for (final SatSolver solver : solvers) {
            solver.add(pg.generate(10));
            final TimeoutHandler handler = Mockito.mock(TimeoutHandler.class);
            final AtomicInteger count = new AtomicInteger(0);
            when(handler.shouldResume(any())).thenReturn(true);
            when(handler.shouldResume(eq(SAT_CONFLICT_DETECTED)))
                    .thenAnswer(invocationOnMock -> count.addAndGet(1) < 5);
            final LngResult<Boolean> result = solver.satCall().handler(handler).sat();
            assertThat(result.isSuccess()).isFalse();
            verify(handler, times(1)).shouldResume(eq(SAT_CALL_STARTED));
            verify(handler, times(5)).shouldResume(eq(SAT_CONFLICT_DETECTED));
            verify(handler, never()).shouldResume(eq(SAT_CALL_FINISHED));
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() {
        for (final SatSolver solver : solvers) {
            solver.add(pg.generate(10));
            final TimeoutHandler handler = new TimeoutHandler(100L);
            final LngResult<Boolean> result = solver.satCall().handler(handler).sat();
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        for (final SatSolver solver : solvers) {
            solver.add(pg.generate(10));
            final TimeoutHandler handler = new TimeoutHandler(System.currentTimeMillis() + 100L, FIXED_END);
            final LngResult<Boolean> result = solver.satCall().handler(handler).sat();
            assertThat(result.isSuccess()).isFalse();
        }
    }
}
