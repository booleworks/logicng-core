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
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewLowerBoundEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewUpperBoundEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSATResult;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class PartialMaxSATTest extends TestWithExampleFormulas {

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

    public PartialMaxSATTest() throws FileNotFoundException {
        logStream = new PrintStream("src/test/resources/partialmaxsat/log.txt");
    }

    @Test
    public void testExceptionalBehaviorForMSU3() {
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.msu3(f);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 2);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently algorithm MSU3 does not support weighted MaxSAT instances.");
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.msu3(f, MaxSATConfig.builder()
                    .incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                    .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER)
                    .build());
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently iterative encoding in MSU3 only supports the Totalizer encoding.");
    }

    @Test
    @LongRunningTag
    public void testWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testOLL() throws IOException {
        for (int i = 0; i < files.length; i++) {
            final MaxSATSolver solver = MaxSATSolver.oll(f);
            readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(results[i]);
        }
    }

    @Test
    @LongRunningTag
    public void testIncWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.incWBO(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testLinearSU() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[4];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[3] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.linearSU(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testLinearUS() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.linearUS(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testMSU3() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.msu3(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testTimeoutHandlerWBO() {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    public void testTimeoutHandlerIncWBO() {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerLinearSU() {
        final MaxSATConfig[] configs = new MaxSATConfig[4];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[3] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerLinearUS() {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerMSU3() {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        configs[2] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(MaxSATConfig.Verbosity.SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    private void testTimeoutHandler(final MaxSATSolver solver) {
        final TimeoutHandler handler = new TimeoutHandler(1000L);

        final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);
        final Formula formula = pg.generate(10);
        solver.addHardFormula(formula);
        solver.addSoftFormula(f.or(formula.variables(f)), 1);
        LNGResult<MaxSATResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isFalse();

        final TimeoutHandler handler2 = new TimeoutHandler(1000L);
        solver.reset();
        solver.addHardFormula(IMP1);
        solver.addSoftFormula(AND1, 1);
        result = solver.solve(handler2);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
    }

    @Test
    public void testTimeoutHandlerSimple() throws IOException {
        MaxSATSolver solver = MaxSATSolver.wbo(f,
                MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/partialmaxsat/c1355_F176gat-1278gat@1.wcnf");
        MaxSatTimeoutHandlerWithApproximation handler = new MaxSatTimeoutHandlerWithApproximation(1000L);
        LNGResult<MaxSATResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isFalse();
        assertThat(handler.lowerBoundApproximation).isLessThan(13);

        solver = MaxSATSolver.wbo(f,
                MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/partialmaxsat/c1355_F1229gat@1.wcnf");
        handler = new MaxSatTimeoutHandlerWithApproximation(5000L);
        result = solver.solve(handler);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
    }

    @Test
    public void testTimeoutHandlerUB() throws IOException {
        final MaxSATSolver solver = MaxSATSolver.linearSU(f,
                MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/partialmaxsat/c1355_F1229gat@1.wcnf");
        final MaxSatTimeoutHandlerWithApproximation handler = new MaxSatTimeoutHandlerWithApproximation(5000L);
        final LNGResult<MaxSATResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
        assertThat(result.getResult().getOptimum()).isEqualTo(handler.upperBoundApproximation);
    }

    @Test
    public void testNonClauselSoftConstraints() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[2];
        solvers[0] = MaxSATSolver.msu3(f);
        solvers[1] = MaxSATSolver.linearUS(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 1);
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getOptimum()).isEqualTo(1);
        }
    }

    @Test
    public void testSoftConstraintsCornerCaseVerum() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[2];
        solvers[0] = MaxSATSolver.msu3(f);
        solvers[1] = MaxSATSolver.linearUS(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("$true"), 1);
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 1);
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getOptimum()).isEqualTo(1);
        }
    }

    @Test
    public void testSoftConstraintsCornerCaseFalsum() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[2];
        solvers[0] = MaxSATSolver.msu3(f);
        solvers[1] = MaxSATSolver.linearUS(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("$false"), 1);
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 1);
            final MaxSATResult result = solver.solve();
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
        public boolean shouldResume(final LNGEvent event) {
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
