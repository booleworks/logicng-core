// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CcExoTest implements LogicNGTest {

    private final EncoderConfig[] configs;

    public CcExoTest() {
        configs = new EncoderConfig[11];
        configs[0] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.PURE).build();
        configs[1] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.LADDER).build();
        configs[2] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.PRODUCT).build();
        configs[3] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.BINARY).build();
        configs[4] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.NESTED).build();
        configs[5] =
                EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.COMMANDER).commanderGroupSize(3).build();
        configs[6] =
                EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.COMMANDER).commanderGroupSize(7).build();
        configs[7] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BimanderGroupSize.FIXED).build();
        configs[8] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BimanderGroupSize.HALF).build();
        configs[9] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.BIMANDER)
                .bimanderGroupSize(EncoderConfig.BimanderGroupSize.SQRT).build();
        configs[10] = EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.BEST).build();
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
        for (final EncoderConfig config : configs) {
            assertThat(CcEncoder.encode(f, cc, config)).containsExactly(f.variable("v0"));
        }
        assertThat(f.newCcVariable().getName()).endsWith("_0");
    }

    @Test
    public void testEXOK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final EncoderConfig config : configs) {
            if (config != null) {
                f.putConfiguration(config);
                testEXO(f, 2);
                testEXO(f, 10);
                testEXO(f, 100);
                testEXO(f, 250);
                testEXO(f, 500);
                assertThat(f.newCcVariable().getName()).endsWith("_" + counter++);
            }
        }
    }

    @Test
    public void testEncodingSetting() {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.PURE).build());
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
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(f.exo(problemLits));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(List.of(problemLits)))
                .hasSize(numLits)
                .allMatch(m -> m.positiveVariables().size() == 1);
    }
}
