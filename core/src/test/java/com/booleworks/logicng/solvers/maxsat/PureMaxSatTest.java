// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.MaxSATReader.readCnfToSolver;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Verbosity.SOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class PureMaxSatTest extends TestWithExampleFormulas {

    private static final String[] files = new String[]{
            "c5315-bug-gate-0.dimacs.seq.filtered.cnf",
            "c6288-bug-gate-0.dimacs.seq.filtered.cnf",
            "c7552-bug-gate-0.dimacs.seq.filtered.cnf",
            "mot_comb1._red-gate-0.dimacs.seq.filtered.cnf",
            "mot_comb2._red-gate-0.dimacs.seq.filtered.cnf",
            "mot_comb3._red-gate-0.dimacs.seq.filtered.cnf",
            "s15850-bug-onevec-gate-0.dimacs.seq.filtered.cnf"
    };
    private final PrintStream logStream;

    public PureMaxSatTest() throws FileNotFoundException {
        logStream = new PrintStream("../test_files/maxsat/log.txt");
    }

    @Test
    public void testExceptionalBehavior() {
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, -1);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The weight of a formula must be > 0");
    }

    @Test
    public void testExceptionalBehaviorForLinearUS() {
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_US);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 3);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently LinearUS does not support weighted MaxSAT instances.");
        assertThatThrownBy(() -> {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                    .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                    .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                    .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                    .build());
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently iterative encoding in LinearUS only supports the Totalizer encoding.");
    }

    @Test
    public void testCornerCase() throws ParserException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO);
        solver.addHardFormula(f.parse("a | b"));
        solver.addHardFormula(f.verum());
        solver.addSoftFormula(A, 1);
        MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
    }

    @Test
    @LongRunningTag
    public void testWBO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .symmetry(true)
                .verbosity(SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .symmetry(false)
                .verbosity(SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (final String file : files) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                MaxSATReader.readCnfToSolver(solver, "../test_files/maxsat/" + file);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
            MaxSATReader.readCnfToSolver(solver, "../test_files/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testIncWBO() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .symmetry(true).verbosity(SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .weight(MaxSatConfig.WeightStrategy.NONE)
                .symmetry(false).verbosity(SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (final String file : files) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                MaxSATReader.readCnfToSolver(solver, "../test_files/maxsat/" + file);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
            MaxSATReader.readCnfToSolver(solver, "../test_files/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testLinearSU() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[2];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER)
                .verbosity(SOME)
                .output(logStream).build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_SU)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                .verbosity(SOME)
                .output(logStream).build();
        for (final MaxSatConfig config : configs) {
            for (final String file : files) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                MaxSATReader.readCnfToSolver(solver, "../test_files/maxsat/" + file);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
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
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.LINEAR_US)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build();
        for (final MaxSatConfig config : configs) {
            for (final String file : files) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                MaxSATReader.readCnfToSolver(solver, "../test_files/maxsat/" + file);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
            MaxSATReader.readCnfToSolver(solver, "../test_files/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testMSU3() throws IOException {
        final MaxSatConfig[] configs = new MaxSatConfig[3];
        configs[0] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build();
        configs[1] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.NONE)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build();
        configs[2] = MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.MSU3)
                .incremental(MaxSatConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSatConfig.CardinalityEncoding.TOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build();
        for (final MaxSatConfig config : configs) {
            for (final String file : files) {
                final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
                MaxSATReader.readCnfToSolver(solver, "../test_files/maxsat/" + file);
                final MaxSatResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
            MaxSATReader.readCnfToSolver(solver, "../test_files/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testOLL() throws IOException {
        for (final String file : files) {
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
            MaxSATReader.readCnfToSolver(solver, "../test_files/maxsat/" + file);
            final MaxSatResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(1);
        }
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
        MaxSATReader.readCnfToSolver(solver, "../test_files/sat/9symml_gr_rcs_w6.shuffled.cnf");
        final MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getOptimum()).isEqualTo(0);
    }

    @Test
    public void testSingle() throws IOException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build());
        readCnfToSolver(solver, "../test_files/maxsat/c-fat200-2.clq.cnf");
        final MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getOptimum()).isEqualTo(26);
        final MaxSat.Stats stats = solver.getStats();
        assertThat(stats.bestSolution()).isEqualTo(26);
        assertThat(stats.unsatCalls()).isEqualTo(26);
        assertThat(stats.satCalls()).isEqualTo(2);
        assertThat(stats.averageCoreSize()).isEqualTo(35.27, Offset.offset(0.01));
        assertThat(stats.symmetryClauses()).isEqualTo(45314);
        assertThat(stats.toString()).isEqualTo(
                "MaxSat.Stats{best solution=26, #sat calls=2, #unsat calls=26, average core size=35.27, #symmetry clauses=45314}");
    }

    @Test
    public void testAssignment() throws ParserException {
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.builder()
                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                .cardinality(MaxSatConfig.CardinalityEncoding.MTOTALIZER)
                .verbosity(SOME)
                .output(logStream)
                .build());
        final PropositionalParser p = new PropositionalParser(f);
        solver.addHardFormula(p.parse("y"));
        solver.addHardFormula(p.parse("~z"));
        solver.addSoftFormula(p.parse("a => b"), 1);
        solver.addSoftFormula(p.parse("b => c"), 1);
        solver.addSoftFormula(p.parse("c => d"), 1);
        solver.addSoftFormula(p.parse("d => e"), 1);
        solver.addSoftFormula(p.parse("a => x"), 1);
        solver.addSoftFormula(p.parse("~e"), 1);
        solver.addSoftFormula(p.parse("~x"), 1);
        solver.addSoftFormula(p.parse("a"), 1);
        solver.addSoftFormula(p.parse("~y"), 1);
        solver.addSoftFormula(p.parse("z"), 1);
        final MaxSatResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getOptimum()).isEqualTo(3);
        final Model model = result.getModel();
        assertThat(model.size()).isEqualTo(8);
        assertThat(model.positiveVariables()).hasSize(1);
        assertThat(model.positiveVariables()).extracting(Variable::getName).containsExactlyInAnyOrder("y");
        assertThat(model.negativeLiterals()).hasSize(7);
        assertThat(model.negativeVariables()).extracting(Variable::getName).containsExactlyInAnyOrder("a", "b",
                "c", "d", "e", "x", "z");
    }

    @Test
    public void testToString() {
        final MaxSatSolver[] solvers = new MaxSatSolver[7];
        solvers[0] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO);
        solvers[1] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_SU);
        solvers[2] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_US);
        solvers[3] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_MSU3);
        solvers[4] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WBO);
        solvers[5] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WMSU3);
        solvers[6] = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);

        final String expected = "MaxSatSolver{result=ComputationResult{" +
                "result=MaxSatResult{satisfiable=true, optimum=1, model=Model{literals=[~a, ~b]}}, " +
                "cancelCause=null}}";

        for (int i = 0; i < solvers.length; i++) {
            final MaxSatSolver solver = solvers[i];
            solver.addHardFormula(OR3);
            solver.addSoftFormula(A, 1);
            if (i == 2 || i == 3) {
                solver.addSoftFormula(NA, 1);
            } else {
                solver.addSoftFormula(NA, 2);
            }
            solver.solve();
            assertThat(solver.toString()).isEqualTo(expected);
        }
    }
}
