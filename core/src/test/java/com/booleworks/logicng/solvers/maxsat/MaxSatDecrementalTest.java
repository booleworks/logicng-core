package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.LINEAR_SU;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.LINEAR_US;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.MSU3;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.WMSU3;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_OLL;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding.MTOTALIZER;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding.TOTALIZER;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy.ITERATIVE;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy.NONE;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.WeightStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatState;
import org.junit.jupiter.api.Test;

public class MaxSatDecrementalTest extends TestWithExampleFormulas {

    @Test
    public void testDecrementalityPartial() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[]{
                MaxSatSolver.newSolver(f, CONFIG_WBO),
                MaxSatSolver.newSolver(f, CONFIG_INC_WBO),
                MaxSatSolver.newSolver(f, CONFIG_OLL),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(TOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(MTOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(TOTALIZER).bmo(true).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(MTOTALIZER).bmo(true).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_US).incremental(NONE).cardinality(TOTALIZER).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_US).incremental(NONE).cardinality(MTOTALIZER).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_US).incremental(ITERATIVE).cardinality(TOTALIZER).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(MSU3).incremental(NONE).cardinality(TOTALIZER).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(MSU3).incremental(NONE).cardinality(MTOTALIZER).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(MSU3).incremental(ITERATIVE).cardinality(TOTALIZER).build())
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
                MaxSatSolver.newSolver(f, builder().algorithm(WBO).weight(WeightStrategy.NONE).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(WBO).weight(WeightStrategy.NORMAL).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(WBO).weight(WeightStrategy.DIVERSIFY).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(INC_WBO).weight(WeightStrategy.NONE).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(INC_WBO).weight(WeightStrategy.NORMAL).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(INC_WBO).weight(WeightStrategy.DIVERSIFY).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(TOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(MTOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(TOTALIZER).bmo(true).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(MTOTALIZER).bmo(true).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(WMSU3).incremental(NONE).cardinality(TOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(WMSU3).incremental(NONE).cardinality(MTOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(WMSU3).incremental(ITERATIVE).cardinality(TOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(WMSU3).incremental(ITERATIVE).cardinality(TOTALIZER).bmo(true).build()),
                MaxSatSolver.newSolver(f, CONFIG_OLL)
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
