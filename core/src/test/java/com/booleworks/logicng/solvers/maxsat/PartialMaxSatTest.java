// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.MaxSATReader.readCnfToSolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewLowerBoundEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewUpperBoundEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;

public class PartialMaxSatTest extends TestWithExampleFormulas {

    private static final String[] files = new String[]{
            "c1355_F176gat-1278gat@1.wcnf",
            "c1355_F1001gat-1048gat@1.wcnf",
            "c1355_F1183gat-1262gat@1.wcnf",
            "c1355_F1229gat@1.wcnf",
            "normalized-s3-3-3-1pb.wcnf",
            "normalized-s3-3-3-2pb.wcnf",
            "normalized-s3-3-3-3pb.wcnf",
            "term1_gr_2pin_w4.shuffled.cnf"
    };
    private static final int[] results = new int[]{
            13, 21, 33, 33, 36, 36, 36, 0
    };
    private final PrintStream logStream;

    public PartialMaxSatTest() throws FileNotFoundException {
        logStream = new PrintStream("../test_files/partialmaxsat/log.txt");
    }

    @Test
    public void testExceptionalBehaviorForMSU3() {
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_MSU3);
            solver.addHardFormula(parse(f, "a | b"));
            solver.addSoftFormula(A, 2);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently algorithm MSU3 does not support weighted MaxSAT instances.");
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                    .algorithm(MaxSatConfig.Algorithm.MSU3)
                    .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                    .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                    .build());
            solver.addHardFormula(parse(f, "a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently iterative encoding in MSU3 only supports the Totalizer encoding.");
    }

    @Test
    @LongRunningTag
    public void testWBO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[1];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream)
                .build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testOLL() throws IOException {
        for (int i = 0; i < files.length; i++) {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
            readCnfToSolver(solver, "../test_files/partialmaxsat/" + files[i]);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(results[i]);
        }
    }

    @Test
    @LongRunningTag
    public void testIncWBO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[1];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream)
                .build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testLinearSU() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[4];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[3] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testLinearUS() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testMSU3() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testTimeoutHandlerWBO() {
        final MaxSatConfig[] configs = new MaxSatConfig[1];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    public void testTimeoutHandlerIncWBO() {
        final MaxSatConfig[] configs = new MaxSatConfig[1];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerLinearSU() {
        final MaxSatConfig[] configs = new MaxSatConfig[4];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[3] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerLinearUS() {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerMSU3() {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    private void testTimeoutHandler(final Supplier<MaxSatSolver> solverGenerator) {
        final TimeoutHandler handler = new TimeoutHandler(1000L);

        final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);
        final Formula formula = pg.generate(10);
        MaxSatSolver solver = solverGenerator.get();
        solver.addHardFormula(formula);
        solver.addSoftFormula(f.or(formula.variables(f)), 1);
        LngResult<MaxSatResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isFalse();

        final TimeoutHandler handler2 = new TimeoutHandler(1000L);
        solver = solverGenerator.get();
        solver.addHardFormula(IMP1);
        solver.addSoftFormula(AND1, 1);
        result = solver.solve(handler2);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
    }

    @Test
    public void testTimeoutHandlerSimple() throws IOException {
        MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream)
                .build());
        readCnfToSolver(solver, "../test_files/partialmaxsat/c1355_F176gat-1278gat@1.wcnf");
        MaxSatTimeoutHandlerWithApproximation handler = new MaxSatTimeoutHandlerWithApproximation(1000L);
        LngResult<MaxSatResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isFalse();
        assertThat(handler.lowerBoundApproximation).isLessThan(13);

        solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .verbosity(MaxSatConfig.Verbosity.SOME)
                .output(logStream)
                .build());
        readCnfToSolver(solver, "../test_files/partialmaxsat/c1355_F1229gat@1.wcnf");
        handler = new MaxSatTimeoutHandlerWithApproximation(5000L);
        result = solver.solve(handler);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
    }

    @Test
    public void testTimeoutHandlerUB() throws IOException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "../test_files/partialmaxsat/c1355_F1229gat@1.wcnf");
        final MaxSatTimeoutHandlerWithApproximation handler = new MaxSatTimeoutHandlerWithApproximation(5000L);
        final LngResult<MaxSatResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
        assertThat(result.getResult().getOptimum()).isEqualTo(handler.upperBoundApproximation);
    }

    @Test
    public void testNonClauselSoftConstraints() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[2];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_MSU3);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_US);
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(parse(f, "a & b & c"));
            solver.addSoftFormula(parse(f, "~a & ~b & ~c"), 1);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getOptimum()).isEqualTo(1);
        }
    }

    @Test
    public void testSoftConstraintsCornerCaseVerum() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[2];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_MSU3);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_US);
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(parse(f, "a & b & c"));
            solver.addSoftFormula(parse(f, "$true"), 1);
            solver.addSoftFormula(parse(f, "~a & ~b & ~c"), 1);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getOptimum()).isEqualTo(1);
        }
    }

    @Test
    public void testSoftConstraintsCornerCaseFalsum() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[2];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_MSU3);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_US);
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(parse(f, "a & b & c"));
            solver.addSoftFormula(parse(f, "$false"), 1);
            solver.addSoftFormula(parse(f, "~a & ~b & ~c"), 1);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getOptimum()).isEqualTo(2);
        }
    }

    static class MaxSatTimeoutHandlerWithApproximation implements ComputationHandler {
        boolean canceled = false;
        long timeout;
        long designatedEnd;
        int lowerBoundApproximation;
        int upperBoundApproximation;

        MaxSatTimeoutHandlerWithApproximation(final long timeout) {
            this.timeout = timeout;
            designatedEnd = 0;
        }

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event instanceof ComputationStartedEvent) {
                if (designatedEnd == 0) {
                    designatedEnd = System.currentTimeMillis() + timeout;
                }
            } else if (event instanceof MaxSatNewLowerBoundEvent) {
                lowerBoundApproximation = ((MaxSatNewLowerBoundEvent) event).getBound();
            } else if (event instanceof MaxSatNewUpperBoundEvent) {
                upperBoundApproximation = ((MaxSatNewUpperBoundEvent) event).getBound();
            }
            canceled = System.currentTimeMillis() >= designatedEnd;
            return !canceled;
        }
    }
}
