// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.MaxSATReader.readCnfToSolver;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity.SOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.solvers.MaxSATResult;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class PureMaxSATTest extends TestWithExampleFormulas {

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

    public PureMaxSATTest() throws FileNotFoundException {
        logStream = new PrintStream("src/test/resources/maxsat/log.txt");
    }

    @Test
    public void testExceptionalBehavior() {
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.incWBO(f);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
            solver.addHardFormula(B);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "The MaxSAT solver does currently not support an incremental interface.  Reset the solver.");
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.incWBO(f);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
            solver.addSoftFormula(B, 1);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "The MaxSAT solver does currently not support an incremental interface.  Reset the solver.");
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.incWBO(f);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, -1);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The weight of a formula must be > 0");
    }

    @Test
    public void testExceptionalBehaviorForLinearUS() {
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.linearUS(f);
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 3);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently LinearUS does not support weighted MaxSAT instances.");
        assertThatThrownBy(() -> {
            final MaxSATSolver solver = MaxSATSolver.linearUS(f, MaxSATConfig.builder()
                    .incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                    .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER)
                    .build());
            solver.addHardFormula(f.parse("a | b"));
            solver.addSoftFormula(A, 1);
            solver.solve();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Error: Currently iterative encoding in LinearUS only supports the Totalizer encoding.");
    }

    @Test
    public void testCornerCase() throws ParserException {
        final MaxSATSolver solver = MaxSATSolver.incWBO(f);
        solver.addHardFormula(f.parse("a | b"));
        solver.addHardFormula(f.verum());
        solver.addSoftFormula(A, 1);
        MaxSATResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
    }

    @Test
    @LongRunningTag
    public void testWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE).symmetry(true).verbosity(SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE).symmetry(false).verbosity(SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (final String file : files) {
                final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
                MaxSATReader.readCnfToSolver(solver, "src/test/resources/maxsat/" + file);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            MaxSATReader.readCnfToSolver(solver, "src/test/resources/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testIncWBO() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE).symmetry(true).verbosity(SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().weight(MaxSATConfig.WeightStrategy.NONE).symmetry(false).verbosity(SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (final String file : files) {
                final MaxSATSolver solver = MaxSATSolver.incWBO(f, config);
                MaxSATReader.readCnfToSolver(solver, "src/test/resources/maxsat/" + file);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            MaxSATReader.readCnfToSolver(solver, "src/test/resources/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testLinearSU() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[2];
        configs[0] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(SOME)
                .output(logStream).build();
        configs[1] = MaxSATConfig.builder().cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(SOME)
                .output(logStream).build();
        for (final MaxSATConfig config : configs) {
            for (final String file : files) {
                final MaxSATSolver solver = MaxSATSolver.linearSU(f, config);
                MaxSATReader.readCnfToSolver(solver, "src/test/resources/maxsat/" + file);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            MaxSATReader.readCnfToSolver(solver, "src/test/resources/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testLinearUS() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] =
                MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                        .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(SOME).output(logStream)
                        .build();
        configs[1] =
                MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                        .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(SOME).output(logStream)
                        .build();
        configs[2] =
                MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                        .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(SOME).output(logStream)
                        .build();
        for (final MaxSATConfig config : configs) {
            for (final String file : files) {
                final MaxSATSolver solver = MaxSATSolver.linearUS(f, config);
                MaxSATReader.readCnfToSolver(solver, "src/test/resources/maxsat/" + file);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            MaxSATReader.readCnfToSolver(solver, "src/test/resources/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testMSU3() throws IOException {
        final MaxSATConfig[] configs = new MaxSATConfig[3];
        configs[0] =
                MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                        .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(SOME).output(logStream)
                        .build();
        configs[1] =
                MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.NONE)
                        .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(SOME).output(logStream)
                        .build();
        configs[2] =
                MaxSATConfig.builder().incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                        .cardinality(MaxSATConfig.CardinalityEncoding.TOTALIZER).verbosity(SOME).output(logStream)
                        .build();
        for (final MaxSATConfig config : configs) {
            for (final String file : files) {
                final MaxSATSolver solver = MaxSATSolver.msu3(f, config);
                MaxSATReader.readCnfToSolver(solver, "src/test/resources/maxsat/" + file);
                final MaxSATResult result = solver.solve();
                assertThat(result.isSatisfiable()).isTrue();
                assertThat(result.getOptimum()).isEqualTo(1);
            }
            final MaxSATSolver solver = MaxSATSolver.wbo(f, config);
            MaxSATReader.readCnfToSolver(solver, "src/test/resources/sat/9symml_gr_rcs_w6.shuffled.cnf");
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(0);
        }
    }

    @Test
    @LongRunningTag
    public void testOLL() throws IOException {
        for (final String file : files) {
            final MaxSATSolver solver = MaxSATSolver.oll(f);
            MaxSATReader.readCnfToSolver(solver, "src/test/resources/maxsat/" + file);
            final MaxSATResult result = solver.solve();
            assertThat(result.isSatisfiable()).isTrue();
            assertThat(result.getOptimum()).isEqualTo(1);
        }
        final MaxSATSolver solver = MaxSATSolver.oll(f);
        MaxSATReader.readCnfToSolver(solver, "src/test/resources/sat/9symml_gr_rcs_w6.shuffled.cnf");
        final MaxSATResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getOptimum()).isEqualTo(0);
    }

    @Test
    public void testSingle() throws IOException {
        final MaxSATSolver solver = MaxSATSolver.incWBO(f, MaxSATConfig.builder()
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(SOME).output(logStream).build());
        readCnfToSolver(solver, "src/test/resources/maxsat/c-fat200-2.clq.cnf");
        final MaxSATResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getOptimum()).isEqualTo(26);
        final MaxSAT.Stats stats = solver.stats();
        assertThat(stats.bestSolution()).isEqualTo(26);
        assertThat(stats.unsatCalls()).isEqualTo(26);
        assertThat(stats.satCalls()).isEqualTo(2);
        assertThat(stats.averageCoreSize()).isEqualTo(28.73, Offset.offset(0.01));
        assertThat(stats.symmetryClauses()).isEqualTo(20359);
        assertThat(stats.toString()).isEqualTo(
                "MaxSAT.Stats{best solution=26, #sat calls=2, #unsat calls=26, average core size=28.73, #symmetry clauses=20359}");
    }

    @Test
    public void testAssignment() throws ParserException {
        final MaxSATSolver solver = MaxSATSolver.incWBO(f, MaxSATConfig.builder()
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER).verbosity(SOME).output(logStream).build());
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
        final MaxSATResult result = solver.solve();
        assertThat(result.isSatisfiable()).isTrue();
        assertThat(result.getOptimum()).isEqualTo(3);
        final Assignment model = result.getModel();
        assertThat(model.size()).isEqualTo(8);
        assertThat(model.positiveVariables()).hasSize(1);
        assertThat(model.positiveVariables()).extracting(Variable::name).containsExactlyInAnyOrder("y");
        assertThat(model.negativeLiterals()).hasSize(7);
        assertThat(model.negativeVariables()).extracting(Variable::name).containsExactlyInAnyOrder("a", "b",
                "c", "d", "e", "x", "z");
    }

    @Test
    public void testToString() {
        final MaxSATSolver[] solvers = new MaxSATSolver[6];
        solvers[0] = MaxSATSolver.incWBO(f);
        solvers[1] = MaxSATSolver.linearSU(f);
        solvers[2] = MaxSATSolver.linearUS(f);
        solvers[3] = MaxSATSolver.msu3(f);
        solvers[4] = MaxSATSolver.wbo(f);
        solvers[5] = MaxSATSolver.wmsu3(f);

        final String expected = "MaxSATSolver{result=ComputationResult{" +
                "result=MaxSATResult{satisfiable=true, optimum=1, model=Assignment{pos=[], neg=[~a, ~b]}}, " +
                "abortionEvent=null}, var2index={@SEL_SOFT_0=2, @SEL_SOFT_1=3, a=0, b=1}}";

        for (int i = 0; i < solvers.length; i++) {
            final MaxSATSolver solver = solvers[i];
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
