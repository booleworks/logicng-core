// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.cardinalityconstraints;

import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.BEST;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.BIMANDER;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.BINARY;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.COMMANDER;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.LADDER;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.NESTED;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.PRODUCT;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.AMO_ENCODER.PURE;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.BIMANDER_GROUP_SIZE.FIXED;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.BIMANDER_GROUP_SIZE.HALF;
import static com.booleworks.logicng.cardinalityconstraints.CCConfig.BIMANDER_GROUP_SIZE.SQRT;
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

public class CCAMOTest implements LogicNGTest {

    private final CCConfig[] configs;

    public CCAMOTest() {
        configs = new CCConfig[14];
        configs[0] = CCConfig.builder().amoEncoding(PURE).build();
        configs[1] = CCConfig.builder().amoEncoding(LADDER).build();
        configs[2] = CCConfig.builder().amoEncoding(PRODUCT).build();
        configs[3] = CCConfig.builder().amoEncoding(BINARY).build();
        configs[4] = CCConfig.builder().amoEncoding(NESTED).build();
        configs[5] = CCConfig.builder().amoEncoding(COMMANDER).commanderGroupSize(3).build();
        configs[6] = CCConfig.builder().amoEncoding(COMMANDER).commanderGroupSize(7).build();
        configs[7] = CCConfig.builder().amoEncoding(BIMANDER).bimanderGroupSize(FIXED).build();
        configs[8] = CCConfig.builder().amoEncoding(BIMANDER).bimanderGroupSize(HALF).build();
        configs[9] = CCConfig.builder().amoEncoding(BIMANDER).bimanderGroupSize(SQRT).build();
        configs[10] =
                CCConfig.builder().amoEncoding(BIMANDER).bimanderGroupSize(FIXED).bimanderFixedGroupSize(2).build();
        configs[11] = CCConfig.builder().amoEncoding(NESTED).nestingGroupSize(5).build();
        configs[12] = CCConfig.builder().amoEncoding(PRODUCT).productRecursiveBound(10).build();
        configs[13] = CCConfig.builder().amoEncoding(BEST).build();
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
        for (final CCConfig config : configs) {
            assertThat(CCEncoder.encode(f, cc, config)).isEmpty();
        }
        assertThat(f.newCCVariable().name()).endsWith("_0");
    }

    @Test
    @LongRunningTag
    public void testAMOK() {
        final FormulaFactory f = FormulaFactory.caching();
        int counter = 0;
        for (final CCConfig config : configs) {
            if (config != null) {
                f.putConfiguration(config);
                testAMO(f, 2, false);
                testAMO(f, 10, false);
                testAMO(f, 100, false);
                testAMO(f, 250, false);
                testAMO(f, 500, false);
                assertThat(f.newCCVariable().name()).endsWith("_" + counter++);
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
        assertThat(f.newCCVariable().name()).endsWith("_0");
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
