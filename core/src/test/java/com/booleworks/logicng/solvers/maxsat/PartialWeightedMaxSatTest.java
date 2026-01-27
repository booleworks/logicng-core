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
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;

public class PartialWeightedMaxSatTest extends TestWithExampleFormulas {

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

    public PartialWeightedMaxSatTest() throws FileNotFoundException {
        logStream = new PrintStream("../test_files/partialweightedmaxsat/log.txt");
    }

    @Test
    public void testExceptionalBehaviorForWMSU3() {
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WMSU3);
            solver.addHardFormula(parse(f, "a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently algorithm WMSU3 does not support unweighted MaxSAT instances.");
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                    .algorithm(MaxSatConfig.Algorithm.WMSU3)
                    .bmo(true)
                    .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                    .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                    .build());
            solver.addHardFormula(parse(f, "a | b"));
            solver.addSoftFormula(A, 2);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently iterative encoding in WMSU3 only supports the Totalizer encoding.");
    }

    @Test
    public void testWBO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialweightedmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getUnsatisfiedWeight()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testIncWBO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialweightedmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getUnsatisfiedWeight()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testLinearSU() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cnfMethod(SatSolverConfig.CnfMethod.FACTORY_CNF)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cnfMethod(SatSolverConfig.CnfMethod.FACTORY_CNF)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialweightedmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getUnsatisfiedWeight()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testWMSU3() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < files.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialweightedmaxsat/" + files[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getUnsatisfiedWeight()).isEqualTo(results[i]);
            }
        }
    }

    @Test
    public void testWMSU3BMO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[1];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < bmoFiles.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialweightedmaxsat/bmo/" + bmoFiles[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getUnsatisfiedWeight()).isEqualTo(bmoResults[i]);
            }
        }
    }

    @Test
    public void testLinearSUBMO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (int i = 0; i < bmoFiles.length; i++) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                readCnfToSolver(solver, "../test_files/partialweightedmaxsat/bmo/" + bmoFiles[i]);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getUnsatisfiedWeight()).isEqualTo(bmoResults[i]);
            }
        }
    }

    @Test
    public void testOLL() throws IOException {
        for (int i = 0; i < bmoFiles.length; i++) {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
            readCnfToSolver(solver, "../test_files/partialweightedmaxsat/bmo/" + bmoFiles[i]);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getUnsatisfiedWeight()).isEqualTo(bmoResults[i]);
        }
        for (int i = 0; i < files.length; i++) {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
            readCnfToSolver(solver, "../test_files/partialweightedmaxsat/" + files[i]);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getUnsatisfiedWeight()).isEqualTo(results[i]);
        }
    }

    @Test
    @LongRunningTag
    public void testLargeOLL1() throws IOException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
        readCnfToSolver(solver, "../test_files/partialweightedmaxsat/large/large_industrial.wcnf");
        final MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getUnsatisfiedWeight()).isEqualTo(68974);
    }

    @Test
    @LongRunningTag
    public void testLargeOLL2() throws IOException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
        readCnfToSolver(solver, "../test_files/partialweightedmaxsat/large/t3g3-5555.spn.wcnf");
        final MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getUnsatisfiedWeight()).isEqualTo(1100610);
    }

    @Test
    @LongRunningTag
    public void testOLLWithLargeWeights() throws IOException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
        readCnfToSolver(solver, "../test_files/partialweightedmaxsat/large/large_weights.wcnf");
        final MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getUnsatisfiedWeight()).isEqualTo(90912);
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerWBO() {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerIncWBO() {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.NORMAL)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    public void testTimeoutHandlerLinearSU() {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerWMSU3() {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WMSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(false)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerWMSU3BMO() {
        final MaxSatConfig[] configs = new MaxSatConfig[1];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    public void testTimeoutHandlerLinearSUBMO() {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER).bmo(true)
                .verbosity(MaxSatConfig.Verbosity.SOME).output(logStream).build();
        for (final MaxSatConfig config : configs) {
            testTimeoutHandler(() -> MaxSatSolver.newSolver(f, config));
        }
    }

    @Test
    public void testWeightedNonClauseSoftConstraints() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[4];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_SU);
        solvers[2] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WBO);
        solvers[3] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WMSU3);
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(parse(f, "a & b & c"));
            solver.addSoftFormula(parse(f, "~a & ~b & ~c"), 2);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getUnsatisfiedWeight()).isEqualTo(2);
            assertThat(result.getSatisfiedWeight()).isEqualTo(0);
            assertThat(solver.getFactory()).isEqualTo(f);
        }
    }

    @Test
    public void testWeightedSoftConstraintsCornerCaseVerum() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[4];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_SU);
        solvers[2] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WBO);
        solvers[3] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WMSU3);
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(parse(f, "a & b & c"));
            solver.addSoftFormula(parse(f, "$true"), 2);
            solver.addSoftFormula(parse(f, "~a & ~b & ~c"), 3);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getUnsatisfiedWeight()).isEqualTo(3);
        }
    }

    @Test
    public void testWeightedSoftConstraintsCornerCaseFalsum() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[4];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_SU);
        solvers[2] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WBO);
        solvers[3] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WMSU3);
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(parse(f, "a & b & c"));
            solver.addSoftFormula(parse(f, "$false"), 2);
            solver.addSoftFormula(parse(f, "~a & ~b & ~c"), 3);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getModel().getLiterals()).containsExactlyInAnyOrder(
                    f.variable("a"), f.variable("b"), f.variable("c")
            );
            assertThat(result.getUnsatisfiedWeight()).isEqualTo(5);
            assertThat(result.getSatisfiedWeight()).isEqualTo(0);
        }
    }

    private void testTimeoutHandler(final Supplier<MaxSatSolver> solverGenerator) {
        final TimeoutHandler handler = new TimeoutHandler(1000L);
        final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);
        final Formula formula = pg.generate(10);
        MaxSatSolver solver = solverGenerator.get();
        solver.addHardFormula(formula);
        solver.addSoftFormula(f.or(formula.variables(f)), 10);
        LngResult<MaxSatResult> result = solver.solve(handler);
        assertThat(result.isSuccess()).isFalse();

        final TimeoutHandler handler2 = new TimeoutHandler(1000L);
        solver = solverGenerator.get();
        solver.addHardFormula(IMP1);
        solver.addSoftFormula(AND1, 10);
        result = solver.solve(handler2);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().isSatisfiable()).isTrue();
    }
}
