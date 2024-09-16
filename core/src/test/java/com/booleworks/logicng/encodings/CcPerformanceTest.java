// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class CcPerformanceTest implements LogicNGTest {

    private final EncoderConfig[] configs;

    public CcPerformanceTest() {
        configs = new EncoderConfig[3];
        configs[0] = EncoderConfig.builder().amkEncoding(EncoderConfig.AmkEncoder.TOTALIZER).build();
        configs[1] = EncoderConfig.builder().amkEncoding(EncoderConfig.AmkEncoder.MODULAR_TOTALIZER).build();
        configs[2] = EncoderConfig.builder().amkEncoding(EncoderConfig.AmkEncoder.CARDINALITY_NETWORK).build();
    }

    @Test
    @LongRunningTag
    public void testAMKPerformance() {
        for (final EncoderConfig config : configs) {
            final FormulaFactory f = FormulaFactory.caching();
            f.putConfiguration(config);
            buildAMK(f, 10_000, false);
            assertThat(f.newCcVariable().getName()).endsWith("_0");
        }
    }

    @Test
    public void testAMKPerformanceMiniCard() {
        final FormulaFactory f = FormulaFactory.caching();
        buildAMK(f, 10_000, true);
        assertThat(f.newCcVariable().getName()).endsWith("_0");
    }

    private void buildAMK(final FormulaFactory f, final int numLits, final boolean miniCard) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        for (int i = 10; i < 100; i = i + 10) {
            final CardinalityConstraint cc = (CardinalityConstraint) f.cc(CType.LE, i, problemLits);
            final SatSolver solver =
                    SatSolver.newSolver(f, SatSolverConfig.builder().useAtMostClauses(miniCard).build());
            solver.add(cc);
            assertSolverSat(solver);
            final Model model = solver.satCall().model(Arrays.asList(problemLits));
            assertThat(cc.evaluate(model.toAssignment())).isTrue();
        }
    }
}
