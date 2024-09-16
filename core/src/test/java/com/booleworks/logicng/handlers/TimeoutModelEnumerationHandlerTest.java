// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.FIXED_END;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MODEL_ENUMERATION_STARTED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SAT_CALL_STARTED;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
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
class TimeoutModelEnumerationHandlerTest {

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
    public void testFoundModel() throws InterruptedException {
        final var handler = new TimeoutHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.shouldResume(MODEL_ENUMERATION_STARTED);
        assertThat(handler.shouldResume(new EnumerationFoundModelsEvent(10))).isTrue();
        Thread.sleep(200);
        assertThat(handler.shouldResume(new EnumerationFoundModelsEvent(10))).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        final Formula formula = f.parse("A & B | C");
        for (final SatSolver solver : solvers) {
            solver.add(formula);
            final TimeoutHandler handler = Mockito.mock(TimeoutHandler.class);
            when(handler.shouldResume(any())).thenReturn(true);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder(formula.variables(f)).build();
            solver.execute(me, handler);
            verify(handler, times(1)).shouldResume(MODEL_ENUMERATION_STARTED);
            verify(handler, atLeast(1)).shouldResume(SAT_CALL_STARTED);
        }
    }

    @Test
    public void testThatSatHandlerIsHandledProperly() {
        final Formula formula = pg.generate(10).negate(f);
        for (final SatSolver solver : solvers) {
            solver.add(formula);
            final TimeoutHandler handler = Mockito.mock(TimeoutHandler.class);
            final AtomicInteger count = new AtomicInteger(0);
            when(handler.shouldResume(any())).then(invocationOnMock -> {
                if (invocationOnMock.getArgument(0) == SAT_CALL_STARTED) {
                    count.addAndGet(1);
                }
                return count.get() <= 5;
            });
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder(formula.variables(f)).build();
            solver.execute(me, handler);
            verify(handler, times(1)).shouldResume(MODEL_ENUMERATION_STARTED);
            verify(handler, times(6)).shouldResume(SAT_CALL_STARTED);
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() {
        final Formula formula = pg.generate(10).negate(f);
        for (final SatSolver solver : solvers) {
            solver.add(formula);
            final TimeoutHandler handler = new TimeoutHandler(20L);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder(formula.variables(f)).build();

            final LngResult<List<Model>> result = me.apply(solver, handler);

            assertThat(result.isSuccess()).isFalse();
            assertThatThrownBy(result::getResult).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        final Formula formula = pg.generate(10).negate(f);
        for (final SatSolver solver : solvers) {
            solver.add(formula);
            final TimeoutHandler handler = new TimeoutHandler(System.currentTimeMillis() + 100L, FIXED_END);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder(formula.variables(f)).build();

            final LngResult<List<Model>> result = me.apply(solver, handler);

            assertThat(result.isSuccess()).isFalse();
            assertThatThrownBy(result::getResult).isInstanceOf(IllegalStateException.class);
        }
    }

    // TODO test partial results (does not seem to work well with negated Pigeon Hole)
}
