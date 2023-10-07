// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.LogicNGTest;
import org.logicng.LongRunningTag;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.CType;
import org.logicng.formulas.CardinalityConstraint;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

public class CCPerformanceTest implements LogicNGTest {

    private final CCConfig[] configs;

    public CCPerformanceTest() {
        configs = new CCConfig[3];
        configs[0] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.TOTALIZER).build();
        configs[1] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.MODULAR_TOTALIZER).build();
        configs[2] = CCConfig.builder().amkEncoding(CCConfig.AMK_ENCODER.CARDINALITY_NETWORK).build();
    }

    @Test
    @LongRunningTag
    public void testAMKPerformance() {
        for (final CCConfig config : configs) {
            final FormulaFactory f = FormulaFactory.caching();
            f.putConfiguration(config);
            buildAMK(f, 10_000, false);
            assertThat(f.newCCVariable().name()).endsWith("_0");
        }
    }

    @Test
    public void testAMKPerformanceMiniCard() {
        final FormulaFactory f = FormulaFactory.caching();
        buildAMK(f, 10_000, true);
        assertThat(f.newCCVariable().name()).endsWith("_0");
    }

    private void buildAMK(final FormulaFactory f, final int numLits, final boolean miniCard) {
        final Variable[] problemLits = new Variable[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
        }
        final SATSolver solver = miniCard ? MiniSat.miniCard(f) : MiniSat.miniSat(f);
        for (int i = 10; i < 100; i = i + 10) {
            final CardinalityConstraint cc = (CardinalityConstraint) f.cc(CType.LE, i, problemLits);
            solver.reset();
            solver.add(cc);
            assertSolverSat(solver);
            final Assignment model = solver.model();
            assertThat(cc.evaluate(model)).isTrue();
        }
    }
}
