// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class TimeoutSATHandlerTest {

    private FormulaFactory f;
    private PigeonHoleGenerator pg;
    private SATSolver[] solvers;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        pg = new PigeonHoleGenerator(f);
        solvers = new SATSolver[7];
        solvers[0] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useAtMostClauses(false).build());
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useAtMostClauses(false).build());
        solvers[2] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useBinaryWatchers(true).useLbdFeatures(true).build());
        solvers[3] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useAtMostClauses(true).build());
        solvers[4] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useAtMostClauses(true).build());
        solvers[5] = MiniSat.miniSat(f, MiniSatConfig.builder().useAtMostClauses(false).cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
        solvers[6] = MiniSat.miniSat(f, MiniSatConfig.builder().useAtMostClauses(true).cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
    }

    @Test
    public void testDetectedConflict() throws InterruptedException {
        final TimeoutSATHandler handler = new TimeoutSATHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.started();
        assertThat(handler.detectedConflict()).isTrue();
        Thread.sleep(200);
        assertThat(handler.detectedConflict()).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        for (final SATSolver solver : solvers) {
            solver.add(f.parse("(x => y) & (~x => y) & (y => z) & (z => ~y)"));
            final TimeoutSATHandler handler = Mockito.mock(TimeoutSATHandler.class);

            solver.sat(handler);

            verify(handler, times(1)).started();
            verify(handler, atLeast(1)).detectedConflict();
            verify(handler, times(1)).finishedSolving();
        }
    }

    @Test
    public void testThatDetectedConflictIsHandledProperly() {
        for (final SATSolver solver : solvers) {
            solver.add(pg.generate(10));
            final TimeoutSATHandler handler = Mockito.mock(TimeoutSATHandler.class);
            final AtomicInteger count = new AtomicInteger(0);
            when(handler.detectedConflict()).then(invocationOnMock -> count.addAndGet(1) < 5);

            final Tristate result = solver.sat(handler);

            assertThat(result).isEqualTo(Tristate.UNDEF);

            verify(handler, times(1)).started();
            verify(handler, times(5)).detectedConflict();
            verify(handler, times(1)).finishedSolving();
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() {
        for (final SATSolver solver : solvers) {
            solver.add(pg.generate(10));
            final TimeoutSATHandler handler = new TimeoutSATHandler(100L);

            final Tristate result = solver.sat(handler);

            assertThat(handler.aborted).isTrue();
            assertThat(result).isEqualTo(Tristate.UNDEF);
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        for (final SATSolver solver : solvers) {
            solver.add(pg.generate(10));
            final TimeoutSATHandler handler = new TimeoutSATHandler(System.currentTimeMillis() + 100L, TimeoutHandler.TimerType.FIXED_END);

            final Tristate result = solver.sat(handler);

            assertThat(handler.aborted).isTrue();
            assertThat(result).isEqualTo(Tristate.UNDEF);
        }
    }
}
