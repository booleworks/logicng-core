// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.sat.GlucoseConfig;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class TimeoutModelEnumerationHandlerTest {

    private FormulaFactory f;
    private PigeonHoleGenerator pg;
    private SATSolver[] solvers;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        pg = new PigeonHoleGenerator(f);
        solvers = new SATSolver[8];
        solvers[0] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).build());
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).build());
        solvers[2] = MiniSat.glucose(f, MiniSatConfig.builder().incremental(false).build(), GlucoseConfig.builder().build());
        solvers[3] = MiniSat.miniCard(f, MiniSatConfig.builder().incremental(true).build());
        solvers[4] = MiniSat.miniCard(f, MiniSatConfig.builder().incremental(false).build());
        solvers[5] = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
        solvers[6] = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).auxiliaryVariablesInModels(false).build());
        solvers[7] = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER).auxiliaryVariablesInModels(false).build());
    }

    @Test
    public void testFoundModel() throws InterruptedException {
        final TimeoutModelEnumerationHandler handler = new TimeoutModelEnumerationHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.started();
        assertThat(handler.foundModel(new Assignment())).isTrue();
        Thread.sleep(200);
        assertThat(handler.foundModel(new Assignment())).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        final Formula formula = f.parse("A & B | C");
        for (final SATSolver solver : solvers) {
            solver.add(formula);
            final TimeoutModelEnumerationHandler handler = Mockito.mock(TimeoutModelEnumerationHandler.class);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder().handler(handler).variables(formula.variables(f)).build();

            solver.execute(me);

            verify(handler, times(1)).started();
            verify(handler, times(1)).satHandler();
        }
    }

    @Test
    public void testThatSatHandlerIsHandledProperly() {
        final Formula formula = pg.generate(10).negate(f);
        for (final SATSolver solver : solvers) {
            solver.add(formula);
            final TimeoutSATHandler satHandler = Mockito.mock(TimeoutSATHandler.class);
            final TimeoutModelEnumerationHandler handler = Mockito.mock(TimeoutModelEnumerationHandler.class);
            final AtomicInteger count = new AtomicInteger(0);
            when(handler.satHandler()).then(invocationOnMock -> {
                count.addAndGet(1);
                return satHandler;
            });
            when(handler.foundModel(any(Assignment.class))).thenReturn(true);
            when(handler.aborted()).then(invocationOnMock -> count.get() > 5);
            lenient().when(satHandler.detectedConflict()).thenReturn(true);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder().handler(handler).variables(formula.variables(f)).build();

            solver.execute(me);

            verify(handler, times(1)).started();
            verify(handler, times(6)).satHandler();
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() {
        final Formula formula = pg.generate(10).negate(f);
        for (final SATSolver solver : solvers) {
            solver.add(formula);
            final TimeoutModelEnumerationHandler handler = new TimeoutModelEnumerationHandler(100L);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder().handler(handler).variables(formula.variables(f)).build();

            final List<Assignment> result = solver.execute(me);

            assertThat(handler.aborted).isTrue();
            assertThat(result).isNotNull(); // assignments found so far are returned
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        final Formula formula = pg.generate(10).negate(f);
        for (final SATSolver solver : solvers) {
            solver.add(formula);
            final TimeoutModelEnumerationHandler handler = new TimeoutModelEnumerationHandler(System.currentTimeMillis() + 100L, TimeoutHandler.TimerType.FIXED_END);
            final ModelEnumerationFunction me = ModelEnumerationFunction.builder().handler(handler).variables(formula.variables(f)).build();

            final List<Assignment> result = solver.execute(me);

            assertThat(handler.aborted).isTrue();
            assertThat(result).isNotNull(); // assignments found so far are returned
        }
    }
}
