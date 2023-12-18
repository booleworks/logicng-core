// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MiniSatTest {

    @Test
    public void testMkWithoutConfiguration() {
        final FormulaFactory f = FormulaFactory.caching();
        final MiniSatConfig miniSatConfig = MiniSatConfig.builder().build();
        f.putConfiguration(miniSatConfig);

        final MiniSat miniSat = MiniSat.mk(f, MiniSat.SolverStyle.MINISAT);
        assertThat(miniSat.getStyle()).isEqualTo(MiniSat.SolverStyle.MINISAT);
        Assertions.assertThat(miniSat.getConfig()).isEqualTo(miniSatConfig);
        Assertions.assertThat(miniSat.underlyingSolver()).isInstanceOf(MiniSat2Solver.class);

        final MiniSat glucose = MiniSat.mk(f, MiniSat.SolverStyle.GLUCOSE);
        assertThat(glucose.getStyle()).isEqualTo(MiniSat.SolverStyle.GLUCOSE);
        Assertions.assertThat(glucose.getConfig()).isEqualTo(miniSatConfig);
        Assertions.assertThat(glucose.underlyingSolver()).isInstanceOf(GlucoseSyrup.class);

        final MiniSat minicard = MiniSat.mk(f, MiniSat.SolverStyle.MINICARD);
        assertThat(minicard.getStyle()).isEqualTo(MiniSat.SolverStyle.MINICARD);
        Assertions.assertThat(minicard.getConfig()).isEqualTo(miniSatConfig);
        Assertions.assertThat(minicard.underlyingSolver()).isInstanceOf(MiniCard.class);
    }

    @Test
    public void testMkWithConfiguration() {
        final FormulaFactory f = FormulaFactory.caching();
        final MiniSatConfig miniSatConfig = MiniSatConfig.builder().build();
        final GlucoseConfig glucoseConfig = GlucoseConfig.builder().build();

        final MiniSat miniSat = MiniSat.mk(f, MiniSat.SolverStyle.MINISAT, miniSatConfig, null);
        assertThat(miniSat.getStyle()).isEqualTo(MiniSat.SolverStyle.MINISAT);
        Assertions.assertThat(miniSat.getConfig()).isEqualTo(miniSatConfig);
        Assertions.assertThat(miniSat.underlyingSolver()).isInstanceOf(MiniSat2Solver.class);

        final MiniSat glucose = MiniSat.mk(f, MiniSat.SolverStyle.GLUCOSE, miniSatConfig, glucoseConfig);
        assertThat(glucose.getStyle()).isEqualTo(MiniSat.SolverStyle.GLUCOSE);
        Assertions.assertThat(glucose.getConfig()).isEqualTo(miniSatConfig);
        Assertions.assertThat(glucose.underlyingSolver()).isInstanceOf(GlucoseSyrup.class);

        final MiniSat minicard = MiniSat.mk(f, MiniSat.SolverStyle.MINICARD, miniSatConfig, null);
        assertThat(minicard.getStyle()).isEqualTo(MiniSat.SolverStyle.MINICARD);
        Assertions.assertThat(minicard.getConfig()).isEqualTo(miniSatConfig);
        Assertions.assertThat(minicard.underlyingSolver()).isInstanceOf(MiniCard.class);
    }

    @Test
    public void testAnalyzeFinal() {
        final MiniSat2Solver solver = new MiniSat2Solver();
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.addClause(clause(1, 2, 3), null);
        solver.addClause(clause(-1, -2), null);
        solver.addClause(clause(-1, -3), null);
        solver.addClause(clause(-2, -3), null);
        Assertions.assertThat(solver.solve(null)).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.solve(null, clause(1, 2))).isEqualTo(Tristate.FALSE);
    }

    @Test
    public void testInvalidSaveState() {
        final MiniSat2Solver solver = new MiniSat2Solver(MiniSatConfig.builder().incremental(false).build());
        assertThatThrownBy(solver::saveState).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testInvalidLoadState() {
        final MiniSat2Solver solver = new MiniSat2Solver(MiniSatConfig.builder().incremental(false).build());
        assertThatThrownBy(() -> solver.loadState(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testConfig() {
        Assertions.assertThat(MiniSatConfig.builder().build().type().toString()).isEqualTo("MINISAT");
        assertThat(Arrays.asList(MiniSatConfig.ClauseMinimization.values())
                .contains(MiniSatConfig.ClauseMinimization.valueOf("DEEP"))).isTrue();
    }

    @Test
    public void testAssumptionChecking() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & B"));
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.sat(f.literal("A", true))).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.sat(f.literal("B", true))).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.sat(f.literal("A", false))).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.sat(f.literal("B", false))).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.sat(f.literal("A", true))).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.sat(f.literal("B", true))).isEqualTo(Tristate.TRUE);
        Assertions.assertThat(solver.sat(f.literal("A", false))).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.TRUE);
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
