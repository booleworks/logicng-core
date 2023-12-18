// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;

public class IncDecTest implements LogicNGTest {

    public static Collection<Object[]> solvers() {
        final var f = FormulaFactory.caching();
        final var solvers = new ArrayList<Object[]>();
        solvers.add(new Object[]{MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useAtMostClauses(false).build())});
        solvers.add(new Object[]{MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useAtMostClauses(true).build())});
        solvers.add(new Object[]{MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useBinaryWatchers(true).build())});
        solvers.add(new Object[]{MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).useBinaryWatchers(true).useAtMostClauses(true).build())});
        return solvers;
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testIncDec(final SATSolver s) {
        final var f = s.factory();
        final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);
        s.add(f.variable("a"));
        final SolverState state1 = s.saveState();
        assertThat(state1.toString()).isEqualTo("SolverState{id=0, state=[1, 1, 0, 1, 0, 0]}");
        assertSolverSat(s);
        s.add(pg.generate(5));
        assertSolverUnsat(s);
        s.loadState(state1);
        assertSolverSat(s);
        s.add(f.literal("a", false));
        assertSolverUnsat(s);
        s.loadState(state1);
        assertSolverSat(s);
        s.add(pg.generate(5));
        final SolverState state2 = s.saveState();
        assertThat(state2.toString()).isEqualTo("SolverState{id=1, state=[1, 31, 81, 1, 0, 0]}");
        s.add(pg.generate(4));
        assertSolverUnsat(s);
        s.loadState(state2);
        assertSolverUnsat(s);
        s.loadState(state1);
        assertSolverSat(s);
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testIncDecDeep(final SATSolver s) {
        final var f = s.factory();
        s.add(f.variable("a"));
        final SolverState state1 = s.saveState();
        s.add(f.variable("b"));
        assertSolverSat(s);
        final SolverState state2 = s.saveState();
        s.add(f.literal("a", false));
        assertSolverUnsat(s);
        s.loadState(state1);
        assertThatThrownBy(() -> s.loadState(state2)).isInstanceOf(IllegalArgumentException.class);
        s.add(f.literal("b", false));
        assertSolverSat(s);
        final SolverState state3 = s.saveState();
        s.add(f.literal("a", false));
        assertSolverUnsat(s);
        s.loadState(state3);
        s.add(f.variable("c"));
        final SolverState state4 = s.saveState();
        final SolverState state5 = s.saveState();
        s.loadState(state4);
        assertThatThrownBy(() -> s.loadState(state5)).isInstanceOf(IllegalArgumentException.class);
        assertSolverSat(s);
        s.loadState(state1);
        assertSolverSat(s);
        assertThatThrownBy(() -> s.loadState(state3)).isInstanceOf(IllegalArgumentException.class);
    }
}
