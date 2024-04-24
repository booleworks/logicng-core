// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CcAmkTest implements LogicNGTest {

    private final EncoderConfig[] configs;

    public CcAmkTest() {
        configs = new EncoderConfig[3];
        configs[0] = EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.TOTALIZER).build();
        configs[1] = EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.MODULAR_TOTALIZER).build();
        configs[2] = EncoderConfig.builder().amkEncoding(EncoderConfig.AMK_ENCODER.CARDINALITY_NETWORK).build();
    }

    @Test
    public void testAMK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final EncoderConfig config : configs) {
            f.putConfiguration(config);
            testCC(f, 10, 0, 1, false);
            testCC(f, 10, 1, 11, false);
            testCC(f, 10, 2, 56, false);
            testCC(f, 10, 3, 176, false);
            testCC(f, 10, 4, 386, false);
            testCC(f, 10, 5, 638, false);
            testCC(f, 10, 6, 848, false);
            testCC(f, 10, 7, 968, false);
            testCC(f, 10, 8, 1013, false);
            testCC(f, 10, 9, 1023, false);
            testCC(f, 10, 10, 1024, false);
            testCC(f, 10, 15, 1024, false);
            assertThat(f.newCCVariable().name()).endsWith("_" + counter++);
        }
    }

    @Test
    public void testAMKMiniCard() {
        final FormulaFactory f = FormulaFactory.caching();
        testCC(f, 10, 0, 1, true);
        testCC(f, 10, 1, 11, true);
        testCC(f, 10, 2, 56, true);
        testCC(f, 10, 3, 176, true);
        testCC(f, 10, 4, 386, true);
        testCC(f, 10, 5, 638, true);
        testCC(f, 10, 6, 848, true);
        testCC(f, 10, 7, 968, true);
        testCC(f, 10, 8, 1013, true);
        testCC(f, 10, 9, 1023, true);
        testCC(f, 10, 10, 1024, true);
        testCC(f, 10, 15, 1024, true);
        assertThat(f.newCCVariable().name()).endsWith("_0");
    }

    @Test
    @LongRunningTag
    public void testLargeAMK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final EncoderConfig config : configs) {
            f.putConfiguration(config);
            testCC(f, 150, 2, 1 + 150 + 11175, false);
            assertThat(f.newCCVariable().name()).endsWith("_" + counter++);
        }
    }

    @Test
    public void testLargeAMKMiniCard() {
        final FormulaFactory f = FormulaFactory.caching();
        testCC(f, 150, 2, 1 + 150 + 11175, true);
        assertThat(f.newCCVariable().name()).endsWith("_0");
    }

    private void testCC(final FormulaFactory f, final int numLits, final int rhs, final int expected,
                        final boolean miniCard) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().useAtMostClauses(miniCard).build());
        solver.add(f.cc(CType.LE, rhs, problemLits));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(problemLits))
                .hasSize(expected)
                .allMatch(m -> m.positiveVariables().size() <= rhs);
    }

    @Test
    public void testIllegalCC1() {
        final FormulaFactory f = FormulaFactory.caching();
        final int numLits = 100;
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        assertThatThrownBy(() -> CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.LE, -1, problemLits)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
