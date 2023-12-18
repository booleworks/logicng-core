// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.MaxSATReader.readCnfToSolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.handlers.TimeoutMaxSATHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.assertj.core.api.Assertions;
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
    public void testWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testOLL() throws IOException {
        for (int i = 0; i < files.length; i++) {
            final MaxSATSolver solver = MaxSATSolver.oll(f);
            readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            assertThat(solver.result()).isEqualTo(results[i]);
        }
    }

    @Test
    public void testIncWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.incWBO(f, config);
                readCnfToSolver(solver, "src/test/resources/partialmaxsat/" + files[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
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
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
            }
        }
    }

    @Test
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
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
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
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
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
        final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(1000L);

        final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);
        final Formula formula = pg.generate(10);
        solver.addHardFormula(formula);
        solver.addSoftFormula(f.or(formula.variables(f)), 1);
        MaxSAT.MaxSATResult result = solver.solve(handler);
        assertThat(handler.aborted()).isTrue();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.UNDEF);

        solver.reset();
        solver.addHardFormula(IMP1);
        solver.addSoftFormula(AND1, 1);
        result = solver.solve(handler);
        assertThat(handler.aborted()).isFalse();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
    }

    @Test
    public void testTimeoutHandlerSimple() throws IOException {
        MaxSATSolver solver = MaxSATSolver.wbo(f,
                MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/partialmaxsat/c1355_F176gat-1278gat@1.wcnf");
        TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(1000L);
        MaxSAT.MaxSATResult result = solver.solve(handler);
        assertThat(handler.aborted()).isTrue();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.UNDEF);
        assertThat(handler.lowerBoundApproximation()).isLessThan(13);

        solver = MaxSATSolver.wbo(f,
                MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/partialmaxsat/c1355_F1229gat@1.wcnf");
        handler = new TimeoutMaxSATHandler(5000L);
        result = solver.solve(handler);
        assertThat(handler.aborted()).isFalse();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
    }

    @Test
    public void testTimeoutHandlerUB() throws IOException {
        final MaxSATSolver solver = MaxSATSolver.linearSU(f,
                MaxSATConfig.builder().verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/partialmaxsat/c1355_F1229gat@1.wcnf");
        final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(5000L);
        final MaxSAT.MaxSATResult result = solver.solve(handler);
        assertThat(handler.aborted()).isFalse();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
        assertThat(solver.result()).isEqualTo(handler.upperBoundApproximation());
    }

    @Test
    public void testNonClauselSoftConstraints() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[2];
        solvers[0] = MaxSATSolver.msu3(f);
        solvers[1] = MaxSATSolver.linearUS(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 1);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            Assertions.assertThat(solver.model().literals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(solver.result()).isEqualTo(1);
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
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            Assertions.assertThat(solver.model().literals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(solver.result()).isEqualTo(1);
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
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            Assertions.assertThat(solver.model().literals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(solver.result()).isEqualTo(2);
        }
    }
}
