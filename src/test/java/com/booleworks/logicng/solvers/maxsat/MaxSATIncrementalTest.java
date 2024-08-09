package com.booleworks.logicng.solvers.maxsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.CardinalityEncoding;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.IncrementalStrategy;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.WeightStrategy;
import org.junit.jupiter.api.Test;

public class MaxSATIncrementalTest extends TestWithExampleFormulas {

    @Test
    public void testIncrementalInterfacePartial() throws ParserException {
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
    public void testIncrementalInterfaceWeighted() throws ParserException {
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
                MaxSATSolver.wmsu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSATSolver.wmsu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.NONE).cardinality(CardinalityEncoding.MTOTALIZER).bmo(false).build()),
                MaxSATSolver.wmsu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).bmo(false).build()),
                MaxSATSolver.wmsu3(f, MaxSATConfig.builder().incremental(IncrementalStrategy.ITERATIVE).cardinality(CardinalityEncoding.TOTALIZER).bmo(true).build()),
                MaxSATSolver.oll(f)
        };
        for (final MaxSATSolver solver : solvers) {
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
