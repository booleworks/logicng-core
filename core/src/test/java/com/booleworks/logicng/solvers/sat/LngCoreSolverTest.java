// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class LngCoreSolverTest {

    @Test
    public void testAnalyzeAssumptionConflict() {
        final LngCoreSolver solver = new LngCoreSolver(FormulaFactory.caching(), SatSolverConfig.builder().build());
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.addClause(clause(1, 2, 3), null);
        solver.addClause(clause(-1, -2), null);
        solver.addClause(clause(-1, -3), null);
        solver.addClause(clause(-2, -3), null);
        assertThat(solver.internalSolve(NopHandler.get()).getResult()).isTrue();
        assertThat(solver.internalSolve(NopHandler.get(), clause(1, 2)).getResult()).isFalse();
    }

    @Test
    public void testConfig() {
        assertThat(SatSolverConfig.builder().build().getType().toString()).isEqualTo("SAT");
        assertThat(Arrays.asList(SatSolverConfig.ClauseMinimization.values())
                .contains(SatSolverConfig.ClauseMinimization.valueOf("DEEP"))).isTrue();
    }

    @Test
    public void testAssumptionChecking() {
        final FormulaFactory f = FormulaFactory.caching();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(parse(f, "A & B"));
        assertThat(solver.sat()).isTrue();
        assertThat(solver.satCall().addFormula(f.literal("A", true)).sat().getResult()).isTrue();
        assertThat(solver.satCall().addFormula(f.literal("B", true)).sat().getResult()).isTrue();
        assertThat(solver.satCall().addFormula(f.literal("A", false)).sat().getResult()).isFalse();
        assertThat(solver.satCall().addFormula(f.literal("B", false)).sat().getResult()).isFalse();
        assertThat(solver.satCall().addFormula(f.literal("A", true)).sat().getResult()).isTrue();
        assertThat(solver.satCall().addFormula(f.literal("B", true)).sat().getResult()).isTrue();
        assertThat(solver.satCall().addFormula(f.literal("A", false)).sat().getResult()).isFalse();
        assertThat(solver.sat()).isTrue();
    }

    private LngIntVector clause(final int... lits) {
        final LngIntVector c = new LngIntVector(lits.length);
        for (final int l : lits) {
            c.push(literal(l));
        }
        return c;
    }

    private int literal(final int l) {
        return l < 0 ? (-l * 2) ^ 1 : l * 2;
    }
}
