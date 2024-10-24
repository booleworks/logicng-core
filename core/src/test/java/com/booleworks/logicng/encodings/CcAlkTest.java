// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import org.junit.jupiter.api.Test;

public class CcAlkTest implements LogicNGTest {

    private final EncoderConfig[] configs;

    public CcAlkTest() {
        configs = new EncoderConfig[3];
        configs[0] = EncoderConfig.builder().alkEncoding(EncoderConfig.AlkEncoder.TOTALIZER).build();
        configs[1] = EncoderConfig.builder().alkEncoding(EncoderConfig.AlkEncoder.MODULAR_TOTALIZER).build();
        configs[2] = EncoderConfig.builder().alkEncoding(EncoderConfig.AlkEncoder.CARDINALITY_NETWORK).build();
    }

    @Test
    public void testALK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final EncoderConfig config : configs) {
            f.putConfiguration(config);
            testCC(f, 10, 0, 1024);
            testCC(f, 10, 1, 1023);
            testCC(f, 10, 2, 1013);
            testCC(f, 10, 3, 968);
            testCC(f, 10, 4, 848);
            testCC(f, 10, 5, 638);
            testCC(f, 10, 6, 386);
            testCC(f, 10, 7, 176);
            testCC(f, 10, 8, 56);
            testCC(f, 10, 9, 11);
            testCC(f, 10, 10, 1);
            testCC(f, 10, 12, 0);
            assertThat(f.newCcVariable().getName()).endsWith("_" + counter++);
        }
    }

    private void testCC(final FormulaFactory f, final int numLits, final int rhs, final int expected) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(f.cc(CType.GE, rhs, problemLits));
        if (expected != 0) {
            assertSolverSat(solver);
        } else {
            assertSolverUnsat(solver);
        }
        final ModelEnumerationFunction me = ModelEnumerationFunction.builder(problemLits)
                .configuration(ModelEnumerationConfig.builder()
                        .build())
                .build();

        assertThat(solver.execute(me, new NumberOfModelsHandler(12000)).getResult())
                .hasSize(expected)
                .allMatch(m -> m.positiveVariables().size() >= rhs);
    }

    @Test
    public void testIllegalCC1() {
        final FormulaFactory f = FormulaFactory.caching();
        final int numLits = 100;
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        assertThatThrownBy(() -> CcEncoder.encode(f, (CardinalityConstraint) f.cc(CType.GE, -1, problemLits)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
