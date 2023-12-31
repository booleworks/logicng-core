// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

public class IncDecTest implements LogicNGTest {

    private final FormulaFactory f;
    private final MiniSat[] solvers;
    private final PigeonHoleGenerator pg;

    public IncDecTest() {
        f = FormulaFactory.caching();
        pg = new PigeonHoleGenerator(f);
        solvers = new MiniSat[2];
        solvers[0] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).build());
        solvers[1] = MiniSat.miniCard(f, MiniSatConfig.builder().incremental(true).build());
    }

    @Test
    public void testIncDec() {
        for (final MiniSat s : solvers) {
            s.add(f.variable("a"));
            final SolverState state1 = s.saveState();
            if (s.underlyingSolver() instanceof MiniCard) {
                assertThat(state1.toString()).isEqualTo("SolverState{id=0, state=[1, 1, 0, 0, 1]}");
            } else {
                assertThat(state1.toString()).isEqualTo("SolverState{id=0, state=[1, 1, 0, 0, 1, 0, 0]}");
            }
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
            if (s.underlyingSolver() instanceof MiniCard) {
                assertThat(state2.toString()).isEqualTo("SolverState{id=1, state=[1, 31, 81, 0, 1]}");
            } else {
                assertThat(state2.toString()).isEqualTo("SolverState{id=1, state=[1, 31, 81, 0, 1, 0, 0]}");
            }
            s.add(pg.generate(4));
            assertSolverUnsat(s);
            s.loadState(state2);
            assertSolverUnsat(s);
            s.loadState(state1);
            assertSolverSat(s);
        }
    }

    @Test
    public void testIncDecDeep() {
        for (final SATSolver s : solvers) {
            s.add(f.variable("a"));
            final SolverState state1 = s.saveState();
            s.add(f.variable("b"));
            assertSolverSat(s);
            final SolverState state2 = s.saveState();
            s.add(f.literal("a", false));
            assertSolverUnsat(s);
            s.loadState(state1);
            try {
                s.loadState(state2);
                assert false;
            } catch (final IllegalArgumentException e) {
                // fine
            }
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
            try {
                s.loadState(state5);
                assert false;
            } catch (final IllegalArgumentException e) {
                // fine
            }
            assertSolverSat(s);
            s.loadState(state1);
            assertSolverSat(s);
            try {
                s.loadState(state3);
                assert false;
            } catch (final IllegalArgumentException e) {
                // fine
            }
        }
    }
}
