package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.LINEAR_SU;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Algorithm.WMSU3;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_OLL;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding.MTOTALIZER;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding.TOTALIZER;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy.ITERATIVE;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy.NONE;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.builder;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.WeightStrategy;
import org.junit.jupiter.api.Test;

public class MaxSatIncrementalTest extends TestWithExampleFormulas {

    @Test
    public void testIncrementalityPartial() throws ParserException {
        final MaxSatSolver[] solvers = new MaxSatSolver[]{
                // MaxSatSolver.newSolver(f, CONFIG_WBO),
                // MaxSatSolver.newSolver(f, CONFIG_INC_WBO),
                // MaxSatSolver.newSolver(f, CONFIG_OLL),
                // MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(TOTALIZER).bmo(false).build()),
                MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(MTOTALIZER).bmo(false).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(TOTALIZER).bmo(true).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_SU).cardinality(MTOTALIZER).bmo(true).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_US).incremental(NONE).cardinality(TOTALIZER).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_US).incremental(NONE).cardinality(MTOTALIZER).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(LINEAR_US).incremental(ITERATIVE).cardinality(TOTALIZER).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(MSU3).incremental(NONE).cardinality(TOTALIZER).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(MSU3).incremental(NONE).cardinality(MTOTALIZER).build()),
                // MaxSatSolver.newSolver(f, builder().algorithm(MSU3).incremental(ITERATIVE).cardinality(TOTALIZER).build())
        };
        for (final MaxSatSolver solver : solvers) {
            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(2);
            solver.addSoftFormula(NA, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(3);
            solver.addHardFormula(A);
            solver.addHardFormula(B);
            solver.addHardFormula(C);
            assertThat(solver.solve().isSatisfiable()).isFalse();
        }
    }

    @Test
    public void testIncrementalityWeighted() throws ParserException {
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
            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);
            solver.addSoftFormula(NA, 4);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);
            solver.addHardFormula(A);
            solver.addHardFormula(B);
            solver.addHardFormula(C);
            assertThat(solver.solve().isSatisfiable()).isFalse();
        }
    }
}
