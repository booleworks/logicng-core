// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import org.junit.jupiter.api.Test;

public class CcAmoTest implements LogicNGTest {

    private final EncoderConfig[] configs;

    public CcAmoTest() {
        configs = new EncoderConfig[14];
        configs[0] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PURE).build();
        configs[1] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.LADDER).build();
        configs[2] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PRODUCT).build();
        configs[3] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.BINARY).build();
        configs[4] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.NESTED).build();
        configs[5] =
                EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.COMMANDER).commanderGroupSize(3).build();
        configs[6] =
                EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.COMMANDER).commanderGroupSize(7).build();
        configs[7] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BIMANDER_GROUP_SIZE.FIXED).build();
        configs[8] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BIMANDER_GROUP_SIZE.HALF).build();
        configs[9] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BIMANDER_GROUP_SIZE.SQRT).build();
        configs[10] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BIMANDER_GROUP_SIZE.FIXED).bimanderFixedGroupSize(2).build();
        configs[11] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.NESTED).nestingGroupSize(5).build();
        configs[12] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PRODUCT).productRecursiveBound(10)
                .build();
        configs[13] = EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.BEST).build();
    }

    @Test
    public void testAMO0() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula cc = f.amo();
        assertThat(cc).isEqualTo(f.verum());
    }

    @Test
    public void testAMO1() {
        final FormulaFactory f = FormulaFactory.caching();
        final CardinalityConstraint cc = (CardinalityConstraint) f.amo(f.variable("v0"));
        for (final EncoderConfig config : configs) {
            assertThat(CcEncoder.encode(f, cc, config)).isEmpty();
        }
        assertThat(f.newCCVariable().getName()).endsWith("_0");
    }

    @Test
    @LongRunningTag
    public void testAMOK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final EncoderConfig config : configs) {
            if (config != null) {
                f.putConfiguration(config);
                testAMO(f, 2, false);
                testAMO(f, 10, false);
                testAMO(f, 100, false);
                testAMO(f, 250, false);
                testAMO(f, 500, false);
                assertThat(f.newCCVariable().getName()).endsWith("_" + counter++);
            }
        }
    }

    @Test
    public void testAMOKMiniCard() {
        final FormulaFactory f = FormulaFactory.caching();
        testAMO(f, 2, true);
        testAMO(f, 10, true);
        testAMO(f, 100, true);
        testAMO(f, 250, true);
        testAMO(f, 500, true);
        assertThat(f.newCCVariable().getName()).endsWith("_0");
    }

    private void testAMO(final FormulaFactory f, final int numLits, final boolean miniCard) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().useAtMostClauses(miniCard).build());
        solver.add(f.amo(problemLits));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(problemLits))
                .hasSize(numLits + 1)
                .allMatch(m -> m.positiveVariables().size() <= 1);
    }
}
