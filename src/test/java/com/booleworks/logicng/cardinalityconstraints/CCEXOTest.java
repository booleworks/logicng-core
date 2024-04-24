// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CCEXOTest implements LogicNGTest {

    private final CCConfig[] configs;

    public CCEXOTest() {
        configs = new CCConfig[11];
        configs[0] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.PURE).build();
        configs[1] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.LADDER).build();
        configs[2] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.PRODUCT).build();
        configs[3] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.BINARY).build();
        configs[4] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.NESTED).build();
        configs[5] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.COMMANDER).commanderGroupSize(3).build();
        configs[6] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.COMMANDER).commanderGroupSize(7).build();
        configs[7] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(CCConfig.BIMANDER_GROUP_SIZE.FIXED).build();
        configs[8] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(CCConfig.BIMANDER_GROUP_SIZE.HALF).build();
        configs[9] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.BIMANDER)
                .bimanderGroupSize(CCConfig.BIMANDER_GROUP_SIZE.SQRT).build();
        configs[10] = CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.BEST).build();
    }

    @Test
    public void testEXO0() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula cc = f.exo();
        assertThat(cc).isEqualTo(f.falsum());
    }

    @Test
    public void testEXO1() {
        final FormulaFactory f = FormulaFactory.caching();
        final CardinalityConstraint cc = (CardinalityConstraint) f.exo(f.variable("v0"));
        for (final CCConfig config : configs) {
            Assertions.assertThat(CCEncoder.encode(f, cc, config)).containsExactly(f.variable("v0"));
        }
        assertThat(f.newCCVariable().name()).endsWith("_0");
    }

    @Test
    public void testEXOK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final CCConfig config : configs) {
            if (config != null) {
                f.putConfiguration(config);
                testEXO(f, 2);
                testEXO(f, 10);
                testEXO(f, 100);
                testEXO(f, 250);
                testEXO(f, 500);
                assertThat(f.newCCVariable().name()).endsWith("_" + counter++);
            }
        }
    }

    @Test
    public void testEncodingSetting() {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.PURE).build());
        final CardinalityConstraint exo = (CardinalityConstraint) f
                .exo(IntStream.range(0, 100).mapToObj(i -> f.variable("v" + i)).collect(Collectors.toList()));
        assertThat(exo.cnf(f).variables(f)).hasSize(100);
        assertThat(exo.cnf(f).numberOfOperands()).isEqualTo(4951);
    }

    @Test
    public void testToString() {
        assertThat(configs[0].amoEncoder.toString()).isEqualTo("PURE");
        assertThat(configs[1].amoEncoder.toString()).isEqualTo("LADDER");
        assertThat(configs[2].amoEncoder.toString()).isEqualTo("PRODUCT");
        assertThat(configs[3].amoEncoder.toString()).isEqualTo("BINARY");
        assertThat(configs[4].amoEncoder.toString()).isEqualTo("NESTED");
        assertThat(configs[5].amoEncoder.toString()).isEqualTo("COMMANDER");
        assertThat(configs[7].amoEncoder.toString()).isEqualTo("BIMANDER");
    }

    private void testEXO(final FormulaFactory f, final int numLits) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.exo(problemLits));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(problemLits))
                .hasSize(numLits)
                .allMatch(m -> m.positiveVariables().size() == 1);
    }
}
