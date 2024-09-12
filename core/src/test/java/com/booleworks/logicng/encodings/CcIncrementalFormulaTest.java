// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import com.booleworks.logicng.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class CcIncrementalFormulaTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final List<SATSolver> solvers;
    private final EncoderConfig[] configs;

    public CcIncrementalFormulaTest() {
        configs = new EncoderConfig[3];
        configs[0] = EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.TOTALIZER)
                .alkEncoding(EncoderConfig.ALK_ENCODER.TOTALIZER).build();
        configs[1] = EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.CARDINALITY_NETWORK)
                .alkEncoding(EncoderConfig.ALK_ENCODER.CARDINALITY_NETWORK).build();
        configs[2] = EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.MODULAR_TOTALIZER)
                .alkEncoding(EncoderConfig.ALK_ENCODER.MODULAR_TOTALIZER).build();
        solvers = SolverTestSet.solverTestSet(Set.of(USE_AT_MOST_CLAUSES), f);
    }

    @Test
    public void testSimpleIncrementalAMK() {
        for (final EncoderConfig config : configs) {
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, 9, vars), config);
            final CcIncrementalData incData = cc.second();

            final SATSolver solver = SATSolver.newSolver(f);
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 4, vars), config)); // >=
            // 4
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 7, vars), config)); // <=
            // 7

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
        for (final EncoderConfig config : configs) {
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LT, 10, vars), config);
            CcIncrementalData incData = cc.second();
            assertThat(incData.toString()).contains("currentRHS=9");

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GT, 1, vars), config);
            incData = cc.second();
            assertThat(incData.toString()).contains("currentRHS=2");

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LT, 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();
            assertThat(cc.first()).contains(vars[0].negate(f));

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, numLits + 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, numLits + 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, numLits, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 0, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 1, vars), config);
            incData = cc.second();
            assertThat(incData).isNull();
        }
    }

    @Test
    public void testSimpleIncrementalALK() {
        for (final EncoderConfig config : configs) {
            final int numLits = 10;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 2, vars), config);
            final CcIncrementalData incData = cc.second();

            final SATSolver solver = SATSolver.newSolver(f);
            // >= 4
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 4, vars), config));
            // <= 7
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 7, vars), config));

            solver.add(cc.first());
            assertSolverSat(solver); // >= 2
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
        for (final SATSolver solver : solvers) {
            final EncoderConfig config = configs[0];
            final int numLits = 100;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars), config);
            final CcIncrementalData incData = cc.second();

            // >= 42
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 42, vars), config));
            solver.add(cc.first());

            // search the lower bound
            while (solver.sat()) {
                // <= currentBound - 1
                solver.add(incData.newUpperBound(--currentBound));
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    public void testLargeTotalizerLowerBoundALK() {
        for (final SATSolver solver : solvers) {
            final EncoderConfig config = configs[0];
            final int numLits = 100;
            int currentBound = 2;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, currentBound, vars), config);
            final CcIncrementalData incData = cc.second();

            // <= 42
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 87, vars), config));
            solver.add(cc.first());

            // search the lower bound
            while (solver.sat()) {
                // <= currentBound + 1
                solver.add(incData.newLowerBound(++currentBound));
            }
            assertThat(currentBound).isEqualTo(88);
        }
    }

    @Test
    @LongRunningTag
    public void testLargeModularTotalizerAMK() {
        for (final SATSolver solver : solvers) {
            final EncoderConfig config = configs[2];
            final int numLits = 100;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars),
                            config);
            final CcIncrementalData incData = cc.second();

            // >= 42
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 42, vars), config));
            solver.add(cc.first());

            // search the lower bound
            while (solver.sat()) {
                // <= currentBound - 1
                solver.add(incData.newUpperBound(--currentBound));
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    @LongRunningTag
    public void testVeryLargeModularTotalizerAMK() {
        for (final SATSolver solver : solvers) {
            final EncoderConfig config = configs[2];
            final int numLits = 300;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars), config);
            final CcIncrementalData incData = cc.second();

            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 234, vars), config));
            solver.add(cc.first());

            // search the lower bound
            while (solver.sat()) {
                solver.add(incData.newUpperBound(--currentBound));
            }
            assertThat(currentBound).isEqualTo(233);
        }
    }
}
