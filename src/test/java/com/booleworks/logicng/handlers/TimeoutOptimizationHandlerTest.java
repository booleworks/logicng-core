// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import com.booleworks.logicng.util.FormulaHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class TimeoutOptimizationHandlerTest {

    private FormulaFactory f;
    private List<SATSolver> solvers;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        solvers = SolverTestSet.solverTestSet(Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD), f);
    }

    @Test
    public void testTimeoutFoundBetterBound() throws InterruptedException {
        final TimeoutOptimizationHandler handler = new TimeoutOptimizationHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.started();
        assertThat(handler.foundBetterBound(Assignment::new)).isTrue();
        Thread.sleep(200);
        assertThat(handler.foundBetterBound(Assignment::new)).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        final Formula formula = f.parse("a & b & (~a => b)");
        for (final SATSolver solver : solvers) {
            solver.add(formula);
            final TimeoutOptimizationHandler handler = Mockito.mock(TimeoutOptimizationHandler.class);

            solver.execute(OptimizationFunction.builder()
                    .handler(handler)
                    .literals(formula.variables(f))
                    .maximize().build());

            verify(handler, times(1)).started();
            verify(handler, atLeast(1)).satHandler();
        }
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() throws IOException {
        final List<Formula> formulas = DimacsReader.readCNF(f, "src/test/resources/sat/too_large_gr_rcs_w5.shuffled.cnf");
        for (final SATSolver solver : solvers) {
            solver.add(formulas);
            final TimeoutOptimizationHandler handler = new TimeoutOptimizationHandler(100L);

            final Assignment result = solver.execute(OptimizationFunction.builder()
                    .handler(handler)
                    .literals(FormulaHelper.variables(f, formulas))
                    .maximize().build());

            assertThat(result).isNull();
        }
    }

    @Test
    public void testTimeoutHandlerFixedEnd() throws IOException {
        final List<Formula> formulas = DimacsReader.readCNF(f, "src/test/resources/sat/too_large_gr_rcs_w5.shuffled.cnf");
        for (final SATSolver solver : solvers) {
            solver.add(formulas);
            final TimeoutOptimizationHandler handler = new TimeoutOptimizationHandler(100L, TimeoutHandler.TimerType.FIXED_END);

            final Assignment result = solver.execute(OptimizationFunction.builder()
                    .handler(handler)
                    .literals(FormulaHelper.variables(f, formulas))
                    .maximize().build());

            assertThat(result).isNull();
        }
    }
}
