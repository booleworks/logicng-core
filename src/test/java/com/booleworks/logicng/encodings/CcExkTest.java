// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import org.junit.jupiter.api.Test;

public class CcExkTest implements LogicNGTest {

    private final EncoderConfig[] configs;

    public CcExkTest() {
        configs = new EncoderConfig[2];
        configs[0] = EncoderConfig.builder().exkEncoding(EncoderConfig.EXK_ENCODER.TOTALIZER).build();
        configs[1] = EncoderConfig.builder().exkEncoding(EncoderConfig.EXK_ENCODER.CARDINALITY_NETWORK).build();
    }

    @Test
    public void testEXK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final EncoderConfig config : configs) {
            f.putConfiguration(config);
            testCC(f, 10, 1, 10);
            testCC(f, 10, 2, 45);
            testCC(f, 10, 3, 120);
            testCC(f, 10, 4, 210);
            testCC(f, 10, 5, 252);
            testCC(f, 10, 6, 210);
            testCC(f, 10, 7, 120);
            testCC(f, 10, 8, 45);
            testCC(f, 10, 9, 10);
            testCC(f, 10, 10, 1);
            testCC(f, 10, 12, 0);
            assertThat(f.newCCVariable().name()).endsWith("_" + counter++);
        }
    }

    private void testCC(final FormulaFactory f, final int numLits, final int rhs, final int expected) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.cc(CType.EQ, rhs, problemLits));
        if (expected != 0) {
            assertSolverSat(solver);
        } else {
            assertSolverUnsat(solver);
        }
        assertThat(solver.enumerateAllModels(problemLits))
                .hasSize(expected)
                .allMatch(m -> m.positiveVariables().size() == rhs);
    }

    @Test
    public void testToString() {
        assertThat(configs[0].exkEncoder.toString()).isEqualTo("TOTALIZER");
        assertThat(configs[1].exkEncoder.toString()).isEqualTo("CARDINALITY_NETWORK");

        assertThat(EncoderConfig.EXK_ENCODER.values())
                .contains(EncoderConfig.EXK_ENCODER.valueOf("CARDINALITY_NETWORK"));
    }
}
