// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class TimeoutMaxSATHandlerTest {

    private FormulaFactory f;
    private List<MaxSATSolver> solvers;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        solvers = Arrays.asList(
                MaxSATSolver.incWBO(f),
                MaxSATSolver.wbo(f),
                MaxSATSolver.linearSU(f),
                MaxSATSolver.linearUS(f),
                MaxSATSolver.msu3(f),
                MaxSATSolver.wmsu3(f),
                MaxSATSolver.oll(f)
        );
    }

    @Test
    public void testTimeoutForLowerBound() throws InterruptedException {
        final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.started();
        assertThat(handler.foundLowerBound(1, new Assignment())).isTrue();
        Thread.sleep(200);
        assertThat(handler.foundLowerBound(1, new Assignment())).isFalse();
    }

    @Test
    public void testTimeoutForUpperBound() throws InterruptedException {
        final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.started();
        assertThat(handler.foundUpperBound(1, new Assignment())).isTrue();
        Thread.sleep(200);
        assertThat(handler.foundUpperBound(1, new Assignment())).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        for (final MaxSATSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            solver.addHardFormula(f.parse("A&B"));
            solver.addSoftFormula(f.parse("~A"), weight);
            solver.addSoftFormula(f.parse("~B"), weight);
            final TimeoutMaxSATHandler handler = Mockito.mock(TimeoutMaxSATHandler.class);
            solver.solve(handler);

            verify(handler, times(1)).started();
            verify(handler, atLeast(1)).satHandler();
            verify(handler, times(1)).finishedSolving();
        }
    }

    @Test
    public void testThatSatHandlerIsHandledProperly() throws IOException {
        final List<Formula> formulas = DimacsReader.readCNF(f, "src/test/resources/sat/unsat/pret60_40.cnf");
        for (final MaxSATSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            formulas.forEach(c -> solver.addSoftFormula(c, weight));
            final TimeoutSATHandler satHandler = Mockito.mock(TimeoutSATHandler.class);
            final TimeoutMaxSATHandler handler = Mockito.mock(TimeoutMaxSATHandler.class);
            when(handler.satHandler()).thenReturn(satHandler);
            lenient().when(handler.foundLowerBound(anyInt(), any())).thenReturn(true);
            lenient().when(handler.foundUpperBound(anyInt(), any())).thenReturn(true);
            final AtomicInteger count = new AtomicInteger(0);
            when(satHandler.detectedConflict()).thenReturn(true);
            when(satHandler.aborted()).then(invocationOnMock -> count.addAndGet(1) > 1);

            final MaxSAT.MaxSATResult solve = solver.solve(handler);

            assertThat(solve).isEqualTo(MaxSAT.MaxSATResult.UNDEF);

            verify(handler, times(1)).started();
            verify(handler, atLeast(1)).satHandler();
            verify(handler, times(1)).finishedSolving();
            verify(satHandler, times(2)).aborted();
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() throws IOException {
        final List<Formula> formulas = DimacsReader.readCNF(f, "src/test/resources/sat/too_large_gr_rcs_w5.shuffled.cnf");
        for (final MaxSATSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            formulas.forEach(c -> solver.addSoftFormula(c, weight));
            final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(10L);

            final MaxSAT.MaxSATResult solve = solver.solve(handler);

            assertThat(handler.aborted()).isTrue();
            assertThat(solve).isEqualTo(MaxSAT.MaxSATResult.UNDEF);
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        final Formula ph = new PigeonHoleGenerator(f).generate(10);
        for (final MaxSATSolver solver : solvers) {
            final int weight = solver.isWeighted() ? 2 : 1;
            ph.forEach(c -> solver.addSoftFormula(c, weight));
            final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(System.currentTimeMillis() + 100L, TimeoutHandler.TimerType.FIXED_END);

            final MaxSAT.MaxSATResult solve = solver.solve(handler);

            assertThat(handler.aborted()).isTrue();
            assertThat(solve).isEqualTo(MaxSAT.MaxSATResult.UNDEF);
        }
    }
}
