// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResultFF;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import com.booleworks.logicng.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class CcIncrementalFormulaTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final List<SatSolver> solvers;
    private final EncoderConfig[] configs;

    public CcIncrementalFormulaTest() {
        configs = new EncoderConfig[3];
        configs[0] = EncoderConfig.builder().amkEncoding(EncoderConfig.AmkEncoder.TOTALIZER)
                .alkEncoding(EncoderConfig.AlkEncoder.TOTALIZER).build();
        configs[1] = EncoderConfig.builder().amkEncoding(EncoderConfig.AmkEncoder.CARDINALITY_NETWORK)
                .alkEncoding(EncoderConfig.AlkEncoder.CARDINALITY_NETWORK).build();
        configs[2] = EncoderConfig.builder().amkEncoding(EncoderConfig.AmkEncoder.MODULAR_TOTALIZER)
                .alkEncoding(EncoderConfig.AlkEncoder.MODULAR_TOTALIZER).build();
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
            final Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, 9, vars), config);
            final CcIncrementalData<EncodingResultFF> incData = cc.getSecond();

            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 4, vars), config)); // >=
            // 4
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 7, vars), config)); // <=
            // 7

            solver.add(cc.getFirst());
            assertSolverSat(solver);
            assertSolverSat(solver); // <= 9
            incData.newUpperBound(8);
            solver.add(incData.getEncodingResult().getResult()); // <= 8
            assertSolverSat(solver);
            assertThat(incData.getCurrentRhs()).isEqualTo(8);
            incData.newUpperBound(7);
            solver.add(incData.getEncodingResult().getResult()); // <= 7
            assertSolverSat(solver);
            incData.newUpperBound(6);
            solver.add(incData.getEncodingResult().getResult()); // <= 6
            assertSolverSat(solver);
            incData.newUpperBound(5);
            solver.add(incData.getEncodingResult().getResult()); // <= 5
            assertSolverSat(solver);
            incData.newUpperBound(4);
            solver.add(incData.getEncodingResult().getResult()); // <= 4
            assertSolverSat(solver);

            final SolverState state = solver.saveState();
            incData.newUpperBound(3);
            solver.add(incData.getEncodingResult().getResult()); // <= 3
            assertSolverUnsat(solver);
            solver.loadState(state);
            assertSolverSat(solver);

            incData.newUpperBound(2);
            solver.add(incData.getEncodingResult().getResult()); // <= 2
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
            Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LT, 10, vars), config);
            CcIncrementalData<EncodingResultFF> incData = cc.getSecond();
            assertThat(incData.toString()).contains("currentRhs=9");

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GT, 1, vars), config);
            incData = cc.getSecond();
            assertThat(incData.toString()).contains("currentRhs=2");

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LT, 1, vars), config);
            incData = cc.getSecond();
            assertThat(incData).isNull();
            assertThat(cc.getFirst()).contains(vars[0].negate(f));

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, numLits + 1, vars), config);
            incData = cc.getSecond();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, numLits + 1, vars), config);
            incData = cc.getSecond();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, numLits, vars), config);
            incData = cc.getSecond();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 0, vars), config);
            incData = cc.getSecond();
            assertThat(incData).isNull();

            cc = CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 1, vars), config);
            incData = cc.getSecond();
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
            final Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, 2, vars), config);
            final CcIncrementalData<EncodingResultFF> incData = cc.getSecond();

            final SatSolver solver = SatSolver.newSolver(f);
            // >= 4
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 4, vars), config));
            // <= 7
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 7, vars), config));

            solver.add(cc.getFirst());
            assertSolverSat(solver); // >= 2
            incData.newLowerBound(3);
            solver.add(incData.getEncodingResult().getResult()); // >= 3
            assertSolverSat(solver);
            incData.newLowerBound(4);
            solver.add(incData.getEncodingResult().getResult()); // >= 4
            assertSolverSat(solver);
            incData.newLowerBound(5);
            solver.add(incData.getEncodingResult().getResult()); // >= 5
            assertSolverSat(solver);
            incData.newLowerBound(6);
            solver.add(incData.getEncodingResult().getResult()); // >= 6
            assertSolverSat(solver);
            incData.newLowerBound(7);
            solver.add(incData.getEncodingResult().getResult()); // >= 7
            assertSolverSat(solver);

            final SolverState state = solver.saveState();
            incData.newLowerBound(8);
            solver.add(incData.getEncodingResult().getResult()); // >= 8
            assertSolverUnsat(solver);
            solver.loadState(state);
            assertSolverSat(solver);
            incData.newLowerBound(9);
            solver.add(incData.getEncodingResult().getResult()); // <= 9
            assertSolverUnsat(solver);
        }
    }

    @Test
    public void testLargeTotalizerUpperBoundAMK() {
        for (final SatSolver solver : solvers) {
            final EncoderConfig config = configs[0];
            final int numLits = 100;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars), config);
            final CcIncrementalData<EncodingResultFF> incData = cc.getSecond();

            // >= 42
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 42, vars), config));
            solver.add(cc.getFirst());

            // search the lower bound
            while (solver.sat()) {
                // <= currentBound - 1
                incData.newUpperBound(--currentBound);
                solver.add(incData.getEncodingResult().getResult());
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    public void testLargeTotalizerLowerBoundALK() {
        for (final SatSolver solver : solvers) {
            final EncoderConfig config = configs[0];
            final int numLits = 100;
            int currentBound = 2;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.GE, currentBound, vars), config);
            final CcIncrementalData<EncodingResultFF> incData = cc.getSecond();

            // <= 42
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, 87, vars), config));
            solver.add(cc.getFirst());

            // search the lower bound
            while (solver.sat()) {
                // <= currentBound + 1
                incData.newLowerBound(++currentBound);
                solver.add(incData.getEncodingResult().getResult());
            }
            assertThat(currentBound).isEqualTo(88);
        }
    }

    @Test
    @LongRunningTag
    public void testLargeModularTotalizerAMK() {
        for (final SatSolver solver : solvers) {
            final EncoderConfig config = configs[2];
            final int numLits = 100;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars),
                            config);
            final CcIncrementalData<EncodingResultFF> incData = cc.getSecond();

            // >= 42
            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 42, vars), config));
            solver.add(cc.getFirst());

            // search the lower bound
            while (solver.sat()) {
                // <= currentBound - 1
                incData.newUpperBound(--currentBound);
                solver.add(incData.getEncodingResult().getResult());
            }
            assertThat(currentBound).isEqualTo(41);
        }
    }

    @Test
    @LongRunningTag
    public void testVeryLargeModularTotalizerAMK() {
        for (final SatSolver solver : solvers) {
            final EncoderConfig config = configs[2];
            final int numLits = 300;
            int currentBound = numLits - 1;
            final Variable[] vars = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                vars[i] = f.variable("v" + i);
            }
            final Pair<List<Formula>, CcIncrementalData<EncodingResultFF>> cc =
                    CcEncoder.encodeIncremental(f, (CardinalityConstraint) f.cc(CType.LE, currentBound, vars), config);
            final CcIncrementalData<EncodingResultFF> incData = cc.getSecond();

            solver.add(CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, 234, vars), config));
            solver.add(cc.getFirst());

            // search the lower bound
            while (solver.sat()) {
                incData.newUpperBound(--currentBound);
                solver.add(incData.getEncodingResult().getResult());
            }
            assertThat(currentBound).isEqualTo(233);
        }
    }
}
