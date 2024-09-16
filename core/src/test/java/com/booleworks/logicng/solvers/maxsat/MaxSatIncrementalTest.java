package com.booleworks.logicng.solvers.maxsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.WeightStrategy;
import org.junit.jupiter.api.Test;

public class MaxSatIncrementalTest extends TestWithExampleFormulas {

    @Test
    public void testIncrementalityPartial() throws ParserException {
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
            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(2);
            solver.addSoftFormula(NA, 1);
            assertThat(solver.solve().getOptimum()).isEqualTo(3);
        }
    }

    @Test
    public void testIncrementalityWeighted() throws ParserException {
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
            solver.addHardFormula(f.parse("(~a | ~b) & (~b | ~c) & ~d"));
            solver.addSoftFormula(A, 1);
            solver.addSoftFormula(B, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(1);
            solver.addSoftFormula(C, 1);
            solver.addSoftFormula(D, 2);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);
            solver.addSoftFormula(NA, 4);
            assertThat(solver.solve().getOptimum()).isEqualTo(4);
        }
    }
}
