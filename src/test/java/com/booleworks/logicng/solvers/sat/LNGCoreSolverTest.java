// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class LNGCoreSolverTest {

    @Test
    public void testAnalyzeAssumptionConflict() {
        final LNGCoreSolver solver = new LNGCoreSolver(FormulaFactory.caching(), SATSolverConfig.builder().build());
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.addClause(clause(1, 2, 3), null);
        solver.addClause(clause(-1, -2), null);
        solver.addClause(clause(-1, -3), null);
        solver.addClause(clause(-2, -3), null);
        Assertions.assertThat(solver.internalSolve()).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.internalSolve(null, clause(1, 2))).isEqualTo(Tristate.FALSE);
    }

    @Test
    public void testConfig() {
        Assertions.assertThat(SATSolverConfig.builder().build().type().toString()).isEqualTo("SAT");
        assertThat(Arrays.asList(SATSolverConfig.ClauseMinimization.values()).contains(SATSolverConfig.ClauseMinimization.valueOf("DEEP"))).isTrue();
    }

    @Test
    public void testAssumptionChecking() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & B"));
        Assertions.assertThat(solver.sat()).isTrue();
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("A", true)).sat()).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("B", true)).sat()).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("A", false)).sat()).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("B", false)).sat()).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("A", true)).sat()).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("B", true)).sat()).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.satCall().addFormulas(f.literal("A", false)).sat()).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.sat()).isTrue();
    }

    private LNGIntVector clause(final int... lits) {
        final LNGIntVector c = new LNGIntVector(lits.length);
        for (final int l : lits) {
            c.push(literal(l));
        }
        return c;
    }

    private int literal(final int l) {
        return l < 0 ? (-l * 2) ^ 1 : l * 2;
    }
}
