// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.LogicNGTest;
import org.logicng.LongRunningTag;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.CType;
import org.logicng.formulas.CardinalityConstraint;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.solvers.SolverState;
import org.logicng.solvers.sat.MiniSatConfig;
import org.logicng.util.Pair;

import java.util.List;

public class CCIncrementalFormulaTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final SATSolver[] solvers;
    private final CCConfig[] configs;

    public CCIncrementalFormulaTest() {
        configs = new CCConfig[3];
        configs[0] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.TOTALIZER).alkEncoding(CCConfig.ALK_ENCODER.TOTALIZER).build();
        configs[1] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.CARDINALITY_NETWORK).alkEncoding(CCConfig.ALK_ENCODER.CARDINALITY_NETWORK).build();
        configs[2] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.MODULAR_TOTALIZER).alkEncoding(CCConfig.ALK_ENCODER.MODULAR_TOTALIZER).build();
        solvers = new SATSolver[4];
        solvers[0] = MiniSat.miniSat(f);
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).build());
        solvers[2] = MiniSat.miniCard(f);
        solvers[3] = MiniSat.glucose(f);
    }

    @Test
    public void testSimpleIncrementalAMK() {
        for (final CCConfig config : configs) {
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, 9, vars), config);
            final CCIncrementalData incData = cc.second();

            final SATSolver solver = MiniSat.miniSat(f);
            solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 4, vars), config)); // >= 4
            solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 7, vars), config)); // <= 7

            solver.add(cc.first());
            assertSolverSat(solver);
            assertSolverSat(solver); // <= 9
            solver.add(incData.newUpperBound(8)); // <= 8
            assertSolverSat(solver);
            assertThat(incData.currentRHS()).isEqualTo(8);
            solver.add(incData.newUpperBound(7)); // <= 7
            assertSolverSat(solver);
            solver.add(incData.newUpperBound(6)); // <= 6
            assertSolverSat(solver);
            solver.add(incData.newUpperBound(5)); // <= 5
            assertSolverSat(solver);
            solver.add(incData.newUpperBound(4)); // <= 4
            assertSolverSat(solver);

            final SolverState state = solver.saveState();
            solver.add(incData.newUpperBound(3)); // <= 3
            assertSolverUnsat(solver);
            solver.loadState(state);
            assertSolverSat(solver);

            solver.add(incData.newUpperBound(2)); // <= 2
            assertSolverUnsat(solver);
        }
    }

    @Test
    public void testIncrementalData() {
        for (final CCConfig config : configs) {
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LT, 10, vars), config);
            CCIncrementalData incData = cc.second();
            assertThat(incData.toString()).contains("currentRHS=9");

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GT, 1, vars), config);
            incData = cc.second();
            assertThat(incData.toString()).contains("currentRHS=2");

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LT, 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();
            assertThat(cc.first()).contains(vars[0].negate(f));

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, numLits + 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, numLits + 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, numLits, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 0, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();
        }
    }

    @Test
    public void testSimpleIncrementalALK() {
        for (final CCConfig config : configs) {
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 2, vars), config);
            final CCIncrementalData incData = cc.second();

            final SATSolver solver = MiniSat.miniSat(f);
            solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 4, vars), config)); // >= 4
            solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 7, vars), config)); // <= 7

            solver.add(cc.first());
            assertSolverSat(solver); // >=2
            solver.add(incData.newLowerBound(3)); // >= 3
            assertSolverSat(solver);
            solver.add(incData.newLowerBound(4)); // >= 4
            assertSolverSat(solver);
            solver.add(incData.newLowerBound(5)); // >= 5
            assertSolverSat(solver);
            solver.add(incData.newLowerBound(6)); // >= 6
            assertSolverSat(solver);
            solver.add(incData.newLowerBound(7)); // >= 7
            assertSolverSat(solver);

            final SolverState state = solver.saveState();
            solver.add(incData.newLowerBound(8)); // >= 8
            assertSolverUnsat(solver);
            solver.loadState(state);
            assertSolverSat(solver);
            solver.add(incData.newLowerBound(9)); // <= 9
            assertSolverUnsat(solver);
        }
    }

    @Test
    public void testLargeTotalizerUpperBoundAMK() {
        final CCConfig config = configs[0];
        final int numLits = 100;
        int currentBound = numLits - 1;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        final Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars), config);
        final CCIncrementalData incData = cc.second();

        final SATSolver solver = solvers[3];
        solver.reset();
        solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 42, vars), config)); // >= 42
        solver.add(cc.first());

        // search the lower bound
        while (solver.sat() == Tristate.TRUE) {
            solver.add(incData.newUpperBound(--currentBound)); // <= currentBound - 1
        }
        assertThat(currentBound).isEqualTo(41);
    }

    @Test
    public void testLargeTotalizerLowerBoundALK() {
        final CCConfig config = configs[0];
        final int numLits = 100;
        int currentBound = 2;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        final Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, currentBound, vars), config);
        final CCIncrementalData incData = cc.second();

        final SATSolver solver = solvers[3];
        solver.reset();
        solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 87, vars), config)); // <= 42
        solver.add(cc.first());

        // search the lower bound
        while (solver.sat() == Tristate.TRUE) {
            solver.add(incData.newLowerBound(++currentBound)); // <= currentBound + 1
        }
        assertThat(currentBound).isEqualTo(88);
    }

    @Test
    @LongRunningTag
    public void testLargeModularTotalizerAMK() {
        for (final SATSolver solver : solvers) {
            final CCConfig config = configs[2];
            final int numLits = 100;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars),
                    config);
            final CCIncrementalData incData = cc.second();

            solver.reset();
            solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 42, vars), config)); // >= 42
            solver.add(cc.first());

            // search the lower bound
            while (solver.sat() == Tristate.TRUE) {
                solver.add(incData.newUpperBound(--currentBound)); // <= currentBound - 1
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    public void testToString() {
        final String expected = String.format("CCConfig{%n" +
                "amoEncoder=BEST%n" +
                "amkEncoder=TOTALIZER%n" +
                "alkEncoder=TOTALIZER%n" +
                "exkEncoder=BEST%n" +
                "bimanderGroupSize=SQRT%n" +
                "bimanderFixedGroupSize=3%n" +
                "nestingGroupSize=4%n" +
                "productRecursiveBound=20%n" +
                "commanderGroupSize=3%n" +
                "}%n");
        assertThat(configs[0].toString()).isEqualTo(expected);
    }

    @Test
    @LongRunningTag
    public void testVeryLargeModularTotalizerAMK() {
        final CCConfig config = configs[2];
        final int numLits = 300;
        int currentBound = numLits - 1;
        final Variable[] vars = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            vars[i] = f.variable("v" + i);
        }
        final Pair<List<Formula>, CCIncrementalData> cc = CCEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars), config);
        final CCIncrementalData incData = cc.second();

        final SATSolver solver = solvers[3];
        solver.reset();
        solver.add(CCEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 234, vars), config));
        solver.add(cc.first());

        // search the lower bound
        while (solver.sat() == Tristate.TRUE) {
            solver.add(incData.newUpperBound(--currentBound));
        }
        assertThat(currentBound).isEqualTo(233);
    }
}
