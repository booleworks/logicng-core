// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class CcIncrementalSolverTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final List<SATSolver> solvers;
    private final EncoderConfig[] configs;

    public CcIncrementalSolverTest() {
        configs = new EncoderConfig[]{
                EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.TOTALIZER)
                        .alkEncoding(EncoderConfig.ALK_ENCODER.TOTALIZER).build(),
                EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.CARDINALITY_NETWORK)
                        .alkEncoding(EncoderConfig.ALK_ENCODER.CARDINALITY_NETWORK).build(),
                EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.MODULAR_TOTALIZER)
                        .alkEncoding(EncoderConfig.ALK_ENCODER.MODULAR_TOTALIZER).build(),
        };
        solvers = SolverTestSet.solverTestSet(Set.of(SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES), f);
    }

    @Test
    public void testSimpleIncrementalAMK() {
        for (final EncoderConfig config : configs) {
            f.putConfiguration(configs[2]);
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().useAtMostClauses(false).build());
            solver.add(f.cc(CType.GE, 4, vars)); // >= 4
            solver.add(f.cc(CType.LE, 7, vars)); // <= 7

            f.putConfiguration(config);

            final CcIncrementalData incData = solver.addIncrementalCc((CardinalityConstraint) f.cc(CType.LE, 9, vars));
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
        for (final SATSolver solver : solvers) {
            final SolverState initialState = solver.saveState();
            for (final EncoderConfig config : configs) {
                solver.loadState(initialState);
                f.putConfiguration(configs[2]);
                final int numLits = 10;
                final Variable[] vars = new Variable[numLits];
                for (int i = 0; i < numLits; i++) {
                    vars[i] = f.variable("v" + i);
                }
                solver.add(f.cc(CType.GE, 4, vars)); // >= 4
                solver.add(f.cc(CType.LE, 7, vars)); // <= 7

                f.putConfiguration(config);

                final CcIncrementalData incData =
                        solver.addIncrementalCc((CardinalityConstraint) f.cc(CType.GE, 2, vars));
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
        for (final SATSolver solver : solvers) {
            solver.add(f.cc(CType.GE, 42, vars)); // >= 42
            f.putConfiguration(configs[0]);
            final CcIncrementalData incData =
                    solver.addIncrementalCc((CardinalityConstraint) f.cc(CType.LE, currentBound, vars));
            // search the lower bound
            while (solver.sat()) {
                incData.newUpperBoundForSolver(--currentBound); // <=
                // currentBound
                // - 1
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    public void testLargeTotalizerLowerBoundALK() {
        final SATSolver solver = SATSolver.newSolver(f);
        f.putConfiguration(configs[2]);
        final int numLits = 100;
        int currentBound = 2;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        solver.add(f.cc(CType.LE, 87, vars));
        f.putConfiguration(configs[0]);
        final CcIncrementalData incData =
                solver.addIncrementalCc((CardinalityConstraint) f.cc(CType.GE, currentBound, vars));
        // search the lower bound
        while (solver.sat()) {
            incData.newLowerBoundForSolver(++currentBound); // <= currentBound +
            // 1
        }
        assertThat(currentBound).isEqualTo(88);
    }

    @Test
    public void testLargeModularTotalizerAMK() {
        for (final SATSolver solver : solvers) {
            f.putConfiguration(configs[2]);
            final int numLits = 100;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            solver.add(f.cc(CType.GE, 42, vars)); // >= 42
            final CcIncrementalData incData =
                    solver.addIncrementalCc((CardinalityConstraint) f.cc(CType.LE, currentBound, vars));
            // search the lower bound
            while (solver.sat()) {
                incData.newUpperBoundForSolver(--currentBound); // <=
                // currentBound
                // - 1
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    @LongRunningTag
    public void testVeryLargeModularTotalizerAMK() {
        for (final SATSolver solver : solvers) {
            f.putConfiguration(configs[2]);
            final int numLits = 300;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            solver.add(f.cc(CType.GE, 234, vars));
            final CcIncrementalData incData =
                    solver.addIncrementalCc((CardinalityConstraint) f.cc(CType.LE, currentBound, vars));
            // search the lower bound
            while (solver.sat()) {
                incData.newUpperBoundForSolver(--currentBound);
            }
            assertThat(currentBound).isEqualTo(233);
        }
    }
}
