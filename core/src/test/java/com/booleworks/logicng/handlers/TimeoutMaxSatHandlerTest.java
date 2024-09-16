// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.FIXED_END;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.SINGLE_TIMEOUT;
import static com.booleworks.logicng.handlers.events.ComputationFinishedEvent.MAX_SAT_CALL_FINISHED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MAX_SAT_CALL_STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.events.MaxSatNewLowerBoundEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewUpperBoundEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class TimeoutMaxSatHandlerTest {

    private FormulaFactory f;
    private List<MaxSatSolver> solvers;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        solvers = Arrays.asList(
                MaxSatSolver.incWbo(f),
                MaxSatSolver.wbo(f),
                MaxSatSolver.linearSu(f),
                MaxSatSolver.linearUs(f),
                MaxSatSolver.msu3(f),
                MaxSatSolver.wmsu3(f),
                MaxSatSolver.oll(f)
        );
    }

    @Test
    public void testTimeoutForLowerBound() throws InterruptedException {
        final TimeoutHandler handler = new TimeoutHandler(100, SINGLE_TIMEOUT);
        handler.shouldResume(MAX_SAT_CALL_STARTED);
        assertThat(handler.shouldResume(new MaxSatNewLowerBoundEvent(1))).isTrue();
        Thread.sleep(200);
        assertThat(handler.shouldResume(new MaxSatNewLowerBoundEvent(1))).isFalse();
    }

    @Test
    public void testTimeoutForUpperBound() throws InterruptedException {
        final TimeoutHandler handler = new TimeoutHandler(100, SINGLE_TIMEOUT);
        handler.shouldResume(MAX_SAT_CALL_STARTED);
        assertThat(handler.shouldResume(new MaxSatNewUpperBoundEvent(1))).isTrue();
        Thread.sleep(200);
        assertThat(handler.shouldResume(new MaxSatNewUpperBoundEvent(1))).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        for (final MaxSatSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            solver.addHardFormula(f.parse("A&B"));
            solver.addSoftFormula(f.parse("~A"), weight);
            solver.addSoftFormula(f.parse("~B"), weight);
            final TimeoutHandler handler = Mockito.mock(TimeoutHandler.class);
            when(handler.shouldResume(any())).thenReturn(true);
            solver.solve(handler);

            verify(handler, times(1)).shouldResume(eq(MAX_SAT_CALL_STARTED));
            verify(handler, times(1)).shouldResume(eq(MAX_SAT_CALL_FINISHED));
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() throws IOException {
        final List<Formula> formulas =
                DimacsReader.readCNF(f, "../test_files/sat/too_large_gr_rcs_w5.shuffled.cnf");
        for (final MaxSatSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            formulas.forEach(c -> solver.addSoftFormula(c, weight));
            final TimeoutHandler handler = new TimeoutHandler(10L);
            final LngResult<MaxSatResult> solve = solver.solve(handler);
            assertThat(solve.isSuccess()).isFalse();
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        final Formula ph = new PigeonHoleGenerator(f).generate(10);
        for (final MaxSatSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            ph.forEach(c -> solver.addSoftFormula(c, weight));
            final TimeoutHandler handler = new TimeoutHandler(System.currentTimeMillis() + 100L, FIXED_END);
            final LngResult<MaxSatResult> solve = solver.solve(handler);
            assertThat(solve.isSuccess()).isFalse();
        }
    }
}
