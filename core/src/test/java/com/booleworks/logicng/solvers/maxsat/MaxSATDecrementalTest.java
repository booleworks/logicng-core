package com.booleworks.logicng.solvers.maxsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.CardinalityEncoding;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.IncrementalStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.WeightStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATState;
import org.junit.jupiter.api.Test;

public class MaxSATDecrementalTest extends TestWithExampleFormulas {

    @Test
    public void testDecrementalityPartial() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[]{
                MaxSATSolver.wbo(f),
                MaxSATSolver.incWBO(f),
                MaxSATSolver.oll(f),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(true).build()),
                MaxSATSolver.linearUS(f, MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).build()),
                MaxSATSolver.linearUS(f, MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).build()),
                MaxSATSolver.linearUS(f, MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).build()),
                MaxSATSolver.msu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).build()),
                MaxSATSolver.msu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).build()),
                MaxSATSolver.msu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).build())
        };
        for (final MaxSATSolver solver : solvers) {
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSATState state0 = solver.saveState();
            assertThat(state0).isEqualTo(new MaxSATState(1, 0, 0, 0, 0, 1, new int[0]));

            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSATState state1 = solver.saveState();
            assertThat(state1).isEqualTo(new MaxSATState(3, 4, 3, 0, 0, 1, new int[0]));

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSATState state2 = solver.saveState();
            assertThat(state2).isEqualTo(new MaxSATState(5, 6, 7, 2, 2, 1, new int[]{1, 1}));

            solver.loadState(state1);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSATState state3 = solver.saveState();
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(2);
            final MaxSATState state4 = solver.saveState();
            solver.addSoftFormula(NA, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(3);

            solver.loadState(state4);
            assertThat(solver.solve().getOptimum()).isEqualTo(2);

            solver.loadState(state3);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);

            assertThatThrownBy(() -> solver.loadState(state2)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("The given solver state is not valid anymore.");

            solver.loadState(state0);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            solver.addSoftFormula(NB, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
        }
    }

    @Test
    public void testDecrementalityWeighted() throws ParserException {
        final MaxSATSolver[] solvers = new MaxSATSolver[]{
                MaxSATSolver.wbo(f, MaxSATConfig.builder().weight(WeightStrategy.NONE).build()),
                MaxSATSolver.wbo(f, MaxSATConfig.builder().weight(WeightStrategy.NORMAL).build()),
                MaxSATSolver.wbo(f, MaxSATConfig.builder().weight(WeightStrategy.DIVERSIFY).build()),
                MaxSATSolver.incWBO(f, MaxSATConfig.builder().weight(WeightStrategy.NONE).build()),
                MaxSATSolver.incWBO(f, MaxSATConfig.builder().weight(WeightStrategy.NORMAL).build()),
                MaxSATSolver.incWBO(f, MaxSATConfig.builder().weight(WeightStrategy.DIVERSIFY).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSATSolver.linearSU(f, MaxSATConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(true).build()),
                MaxSATSolver.wmsu3(f,
                        MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSATSolver.wmsu3(f,
                        MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSATSolver.wmsu3(f,
                        MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSATSolver.wmsu3(f,
                        MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSATSolver.oll(f)
        };
        for (final MaxSATSolver solver : solvers) {
            solver.addSoftFormula(X, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSATState state0 = solver.saveState();
            assertThat(state0).isEqualTo(new MaxSATState(1, 2, 2, 1, 2, 2, new int[]{2}));

            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSATState state1 = solver.saveState();
            assertThat(state1).isEqualTo(new MaxSATState(3, 6, 5, 1, 2, 2, new int[]{2}));

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSATState state2 = solver.saveState();
            assertThat(state2).isEqualTo(new MaxSATState(5, 8, 9, 3, 5, 2, new int[]{2, 1, 2}));

            solver.loadState(state1);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSATState state3 = solver.saveState();
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);
            final MaxSATState state4 = solver.saveState();
            solver.addSoftFormula(NA, 4);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);

            solver.loadState(state4);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);

            solver.loadState(state3);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);

            assertThatThrownBy(() -> solver.loadState(state2)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("The given solver state is not valid anymore.");

            solver.loadState(state0);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            solver.addSoftFormula(NB, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
        }
    }
}
