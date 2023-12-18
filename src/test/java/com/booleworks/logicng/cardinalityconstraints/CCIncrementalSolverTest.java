// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import org.junit.jupiter.api.Test;

public class CCIncrementalSolverTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final SATSolver[] solvers;
    private final CCConfig[] configs;

    public CCIncrementalSolverTest() {
        configs = new CCConfig[3];
        configs[0] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.TOTALIZER)
                .alkEncoding(CCConfig.ALK_ENCODER.TOTALIZER).build();
        configs[1] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.CARDINALITY_NETWORK)
                .alkEncoding(CCConfig.ALK_ENCODER.CARDINALITY_NETWORK).build();
        configs[2] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.MODULAR_TOTALIZER)
                .alkEncoding(CCConfig.ALK_ENCODER.MODULAR_TOTALIZER).build();
        solvers = new SATSolver[4];
        solvers[0] = MiniSat.miniSat(f);
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).build());
        solvers[2] = MiniSat.miniCard(f);
        solvers[3] = MiniSat.glucose(f);
    }

    @Test
    public void testSimpleIncrementalAMK() {
        for (final CCConfig config : configs) {
            f.putConfiguration(configs[2]);
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final SATSolver solver = MiniSat.miniSat(f);
            solver.add(f.cc(CType.GE, 4, vars)); // >= 4
            solver.add(f.cc(CType.LE, 7, vars)); // <= 7

            f.putConfiguration(config);

            final CCIncrementalData incData = solver.addIncrementalCC((CardinalityConstraint) f.cc(CType.LE, 9, vars));
            assertSolverSat(solver); // <= 9
            incData.newUpperBoundForSolver(8); // <= 8
            assertSolverSat(solver);
            incData.newUpperBoundForSolver(7); // <= 7
            assertSolverSat(solver);
            incData.newUpperBoundForSolver(6); // <= 6
            assertSolverSat(solver);
            incData.newUpperBoundForSolver(5); // <= 5
            assertSolverSat(solver);
            incData.newUpperBoundForSolver(4); // <= 4
            assertSolverSat(solver);

            final SolverState state = solver.saveState();
            incData.newUpperBoundForSolver(3); // <= 3
            assertSolverUnsat(solver);
            solver.loadState(state);
            assertSolverSat(solver);

            incData.newUpperBoundForSolver(2); // <= 2
            assertSolverUnsat(solver);
        }
    }

    @Test
    public void testSimpleIncrementalALK() {
        for (final CCConfig config : configs) {
            f.putConfiguration(configs[2]);
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final SATSolver solver = solvers[2];
            solver.reset();
            solver.add(f.cc(CType.GE, 4, vars)); // >= 4
            solver.add(f.cc(CType.LE, 7, vars)); // <= 7

            f.putConfiguration(config);

            final CCIncrementalData incData = solver.addIncrementalCC((CardinalityConstraint) f.cc(CType.GE, 2, vars));
            assertSolverSat(solver); // >=2
            incData.newLowerBoundForSolver(3); // >= 3
            assertSolverSat(solver);
            incData.newLowerBoundForSolver(4); // >= 4
            assertSolverSat(solver);
            incData.newLowerBoundForSolver(5); // >= 5
            assertSolverSat(solver);
            incData.newLowerBoundForSolver(6); // >= 6
            assertSolverSat(solver);
            incData.newLowerBoundForSolver(7); // >= 7
            assertSolverSat(solver);

            final SolverState state = solver.saveState();
            incData.newLowerBoundForSolver(8); // >= 8
            assertSolverUnsat(solver);
            solver.loadState(state);
            assertSolverSat(solver);

            incData.newLowerBoundForSolver(9); // <= 9
            assertSolverUnsat(solver);
        }
    }

    @Test
    public void testLargeTotalizerUpperBoundAMK() {
        f.putConfiguration(configs[2]);
        final int numLits = 100;
        int currentBound = numLits - 1;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        final SATSolver solver = solvers[3];
        solver.reset();
        solver.add(f.cc(CType.GE, 42, vars)); // >= 42
        f.putConfiguration(configs[0]);
        final CCIncrementalData incData =
                solver.addIncrementalCC((CardinalityConstraint) f.cc(CType.LE, currentBound, vars));
        // search the lower bound
        while (solver.sat() == Tristate.TRUE) {
            incData.newUpperBoundForSolver(--currentBound); // <= currentBound -
                                                            // 1
        }
        assertThat(currentBound).isEqualTo(41);
    }

    @Test
    public void testLargeTotalizerLowerBoundALK() {
        f.putConfiguration(configs[2]);
        final int numLits = 100;
        int currentBound = 2;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        final SATSolver solver = solvers[0];
        solver.reset();
        solver.add(f.cc(CType.LE, 87, vars));
        f.putConfiguration(configs[0]);
        final CCIncrementalData incData =
                solver.addIncrementalCC((CardinalityConstraint) f.cc(CType.GE, currentBound, vars));
        // search the lower bound
        while (solver.sat() == Tristate.TRUE) {
            incData.newLowerBoundForSolver(++currentBound); // <= currentBound +
                                                            // 1
        }
        assertThat(currentBound).isEqualTo(88);
    }

    @Test
    @LongRunningTag
    public void testLargeModularTotalizerAMK() {
        for (final SATSolver solver : solvers) {
            if (solver != null) {
                f.putConfiguration(configs[2]);
                final int numLits = 100;
                int currentBound = numLits - 1;
                final Variable[] vars = new Variable[numLits];
                for (int i = 0; i < numLits; i++) {
                    vars[i] = f.variable("v" + i);
                }
                solver.reset();
                solver.add(f.cc(CType.GE, 42, vars)); // >= 42
                final CCIncrementalData incData =
                        solver.addIncrementalCC((CardinalityConstraint) f.cc(CType.LE, currentBound, vars));
                // search the lower bound
                while (solver.sat() == Tristate.TRUE) {
                    incData.newUpperBoundForSolver(--currentBound); // <=
                                                                    // currentBound
                                                                    // - 1
                }
                assertThat(currentBound).isEqualTo(41);
            }
        }
    }

    @Test
    @LongRunningTag
    public void testVeryLargeModularTotalizerAMK() {
        f.putConfiguration(configs[2]);
        final int numLits = 300;
        int currentBound = numLits - 1;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        final SATSolver solver = solvers[3];
        solver.reset();
        solver.add(f.cc(CType.GE, 234, vars));
        final CCIncrementalData incData =
                solver.addIncrementalCC((CardinalityConstraint) f.cc(CType.LE, currentBound, vars));
        // search the lower bound
        while (solver.sat() == Tristate.TRUE) {
            incData.newUpperBoundForSolver(--currentBound);
        }
        assertThat(currentBound).isEqualTo(233);
    }
}
