package com.booleworks.logicng.solvers.maxsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.WeightStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatState;
import org.junit.jupiter.api.Test;

public class MaxSatDecrementalTest extends TestWithExampleFormulas {

    @Test
    public void testDecrementalityPartial() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[]{
                MaxSatSolver.wbo(f),
                MaxSatSolver.incWbo(f),
                MaxSatSolver.oll(f),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(true).build()),
                MaxSatSolver.linearUs(f, MaxSatConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).build()),
                MaxSatSolver.linearUs(f, MaxSatConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).build()),
                MaxSatSolver.linearUs(f, MaxSatConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).build()),
                MaxSatSolver.msu3(f, MaxSatConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).build()),
                MaxSatSolver.msu3(f, MaxSatConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).build()),
                MaxSatSolver.msu3(f, MaxSatConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).build())
        };
        for (final MaxSatSolver solver : solvers) {
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSatState state0 = solver.saveState();
            assertThat(state0).isEqualTo(new MaxSatState(1, 0, 0, 0, 0, 1, new int[0]));

            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSatState state1 = solver.saveState();
            assertThat(state1).isEqualTo(new MaxSatState(3, 4, 3, 0, 0, 1, new int[0]));

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSatState state2 = solver.saveState();
            assertThat(state2).isEqualTo(new MaxSatState(5, 6, 7, 2, 2, 1, new int[]{1, 1}));

            solver.loadState(state1);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSatState state3 = solver.saveState();
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(2);
            final MaxSatState state4 = solver.saveState();
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
        final MaxSatSolver[] solvers = new MaxSatSolver[]{
                MaxSatSolver.wbo(f, MaxSatConfig.builder().weight(WeightStrategy.NONE).build()),
                MaxSatSolver.wbo(f, MaxSatConfig.builder().weight(WeightStrategy.NORMAL).build()),
                MaxSatSolver.wbo(f, MaxSatConfig.builder().weight(WeightStrategy.DIVERSIFY).build()),
                MaxSatSolver.incWbo(f, MaxSatConfig.builder().weight(WeightStrategy.NONE).build()),
                MaxSatSolver.incWbo(f, MaxSatConfig.builder().weight(WeightStrategy.NORMAL).build()),
                MaxSatSolver.incWbo(f, MaxSatConfig.builder().weight(WeightStrategy.DIVERSIFY).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSatSolver.linearSu(f, MaxSatConfig.builder().cardinality(CardinalityEncoding.MTOTALIZER).bmo(true).build()),
                MaxSatSolver.wmsu3(f,
                        MaxSatConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSatSolver.wmsu3(f,
                        MaxSatConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSatSolver.wmsu3(f,
                        MaxSatConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSatSolver.wmsu3(f,
                        MaxSatConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSatSolver.oll(f)
        };
        for (final MaxSatSolver solver : solvers) {
            solver.addSoftFormula(X, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSatState state0 = solver.saveState();
            assertThat(state0).isEqualTo(new MaxSatState(1, 2, 2, 1, 2, 2, new int[]{2}));

            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            assertThat(solver.solve().getOptimum()).isEqualTo(0);
            final MaxSatState state1 = solver.saveState();
            assertThat(state1).isEqualTo(new MaxSatState(3, 6, 5, 1, 2, 2, new int[]{2}));

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSatState state2 = solver.saveState();
            assertThat(state2).isEqualTo(new MaxSatState(5, 8, 9, 3, 5, 2, new int[]{2, 1, 2}));

            solver.loadState(state1);
            assertThat(solver.solve().getOptimum()).isEqualTo(0);

            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            final MaxSatState state3 = solver.saveState();
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);
            final MaxSatState state4 = solver.saveState();
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
