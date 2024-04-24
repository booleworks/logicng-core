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

public class PartialWeightedMaxSATTest extends TestWithExampleFormulas {

    private static final String[] files = new String[]{
            "8.wcsp.log.wcnf",
            "54.wcsp.log.wcnf",
            "404.wcsp.log.wcnf",
            "term1_gr_2pin_w4.shuffled.cnf"
    };
    private static final int[] results = new int[]{
            2, 37, 114, 0
    };
    private static final String[] bmoFiles = new String[]{
            "normalized-factor-size=9-P=11-Q=283.opb.wcnf",
            "normalized-factor-size=9-P=11-Q=53.opb.wcnf",
            "normalized-factor-size=9-P=13-Q=179.opb.wcnf",
            "normalized-factor-size=9-P=17-Q=347.opb.wcnf",
            "normalized-factor-size=9-P=17-Q=487.opb.wcnf",
            "normalized-factor-size=9-P=23-Q=293.opb.wcnf"
    };
    private static final int[] bmoResults = new int[]{
            11, 11, 13, 17, 17, 23
    };
    private final PrintStream logStream;

    public PartialWeightedMaxSATTest() throws FileNotFoundException {
        logStream = new PrintStream("src/test/resources/partialweightedmaxsat/log.txt");
    }

    @Test
    public void testExceptionalBehaviorForWMSU3() {
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.wmsu3(f);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently algorithm WMSU3 does not support unweighted MaxSAT instances.");
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.wmsu3(f, MaxSATConfig.builder()
                    .bmo(true)
                    .incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                    .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER)
                    .build());
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 2);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently iterative encoding in WMSU3 only supports the Totalizer encoding.");
    }

    @Test
    public void testWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
                readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/" + files[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testIncWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.incWBO(f, config);
                readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/" + files[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testLinearSU() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.linearSU(f, config);
                readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/" + files[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testWMSU3() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.wmsu3(f, config);
                readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/" + files[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testWMSU3BMO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < bmoFiles.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.wmsu3(f, config);
                readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/bmo/" + bmoFiles[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(bmoResults[i]);
            }
        }
    }

    @Test
    public void testLineaerSUBMO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (int i = 0; i < bmoFiles.length; i++) {
                final MaxSATSolver solver = MaxSATSolver.linearSU(f, config);
                readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/bmo/" + bmoFiles[i]);
                Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
                assertThat(solver.result()).isEqualTo(bmoResults[i]);
            }
        }
    }

    @Test
    public void testOLL() throws IOException {
        for (int i = 0; i < bmoFiles.length; i++) {
            final MaxSATSolver solver = MaxSATSolver.oll(f);
            readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/bmo/" + bmoFiles[i]);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            assertThat(solver.result()).isEqualTo(bmoResults[i]);
        }
        for (int i = 0; i < files.length; i++) {
            final MaxSATSolver solver = MaxSATSolver.oll(f);
            readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/" + files[i]);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            assertThat(solver.result()).isEqualTo(results[i]);
        }
    }

    @Test
    @LongRunningTag
    public void testLargeOLL1() throws IOException {
        final MaxSATSolver solver = MaxSATSolver.oll(f);
        readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/large/large_industrial.wcnf");
        Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
        assertThat(solver.result()).isEqualTo(68974);
    }

    @Test
    @LongRunningTag
    public void testLargeOLL2() throws IOException {
        final MaxSATSolver solver = MaxSATSolver.oll(f);
        readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/large/t3g3-5555.spn.wcnf");
        Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
        assertThat(solver.result()).isEqualTo(1100610);
    }

    @Test
    @LongRunningTag
    public void testOLLWithLargeWeights() throws IOException {
        final MaxSATSolver solver = MaxSATSolver.oll(f);
        readCnfToSolver(solver, "src/test/resources/partialweightedmaxsat/large/large_weights.wcnf");
        Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
        assertThat(solver.result()).isEqualTo(90912);
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerWBO() {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerIncWBO() {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    public void testTimeoutHandlerLinearSU() {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerWMSU3() {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerWMSU3BMO() {
        final MaxSATConfig[] configs = new MaxSATConfig[1];
        configs[0] = MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    public void testTimeoutHandlerLinearSUBMO() {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSATConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSATConfig config : configs) {
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            testTimeoutHandler(solver);
        }
    }

    @Test
    public void testWeightedNonClauseSoftConstraints() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[4];
        solvers[0] = MaxSATSolver.incWBO(f);
        solvers[1] = MaxSATSolver.linearSU(f);
        solvers[2] = MaxSATSolver.wbo(f);
        solvers[3] = MaxSATSolver.wmsu3(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 2);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            Assertions.assertThat(solver.model().literals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(solver.result()).isEqualTo(2);
            Assertions.assertThat(solver.factory()).isEqualTo(f);
        }
    }

    @Test
    public void testWeightedSoftConstraintsCornerCaseVerum() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[4];
        solvers[0] = MaxSATSolver.incWBO(f);
        solvers[1] = MaxSATSolver.linearSU(f);
        solvers[2] = MaxSATSolver.wbo(f);
        solvers[3] = MaxSATSolver.wmsu3(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("$true"), 2);
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 3);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            Assertions.assertThat(solver.model().literals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(solver.result()).isEqualTo(3);
        }
    }

    @Test
    public void testWeightedSoftConstraintsCornerCaseFalsum() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[4];
        solvers[0] = MaxSATSolver.incWBO(f);
        solvers[1] = MaxSATSolver.linearSU(f);
        solvers[2] = MaxSATSolver.wbo(f);
        solvers[3] = MaxSATSolver.wmsu3(f);
        for (final MaxSATSolver solver : solvers) {
            solver.addHardFormula(f.parse("a & b & c"));
            solver.addSoftFormula(f.parse("$false"), 2);
            solver.addSoftFormula(f.parse("~a & ~b & ~c"), 3);
            Assertions.assertThat(solver.solve()).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
            Assertions.assertThat(solver.model().literals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(solver.result()).isEqualTo(5);
        }
    }

    private void testTimeoutHandler(final MaxSATSolver solver) {
        final TimeoutMaxSATHandler handler = new TimeoutMaxSATHandler(1000L);

        final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);
        final Formula formula = pg.generate(10);
        solver.addHardFormula(formula);
        solver.addSoftFormula(f.or(formula.variables(f)), 10);
        MaxSAT.MaxSATResult result = solver.solve(handler);
        assertThat(handler.aborted()).isTrue();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.UNDEF);

        solver.reset();
        solver.addHardFormula(IMP1);
        solver.addSoftFormula(AND1, 10);
        result = solver.solve(handler);
        assertThat(handler.aborted()).isFalse();
        assertThat(result).isEqualTo(MaxSAT.MaxSATResult.OPTIMUM);
    }
}
