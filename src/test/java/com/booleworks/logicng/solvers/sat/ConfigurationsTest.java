// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;

public class ConfigurationsTest {

    @Test
    public void testSolverConfigToString() {
        final SATSolverLowLevelConfig config = SATSolverLowLevelConfig.builder()
                .varDecay(1.2)
                .varInc(1.3)
                .restartFirst(200)
                .restartInc(0.8)
                .clauseDecay(0.92)
                .lbLBDMinimizingClause(3)
                .lbLBDFrozenClause(25)
                .lbSizeMinimizingClause(24)
                .firstReduceDB(1999)
                .specialIncReduceDB(999)
                .incReduceDB(299)
                .factorK(0.7)
                .factorR(1.3)
                .sizeLBDQueue(45)
                .sizeTrailQueue(4999)
                .reduceOnSize(true)
                .reduceOnSizeSize(10)
                .maxVarDecay(0.99)
                .build();
        final String expected = String.format("SATSolverLowLevelConfig{%n" +
                "varDecay=1.2%n" +
                "varInc=1.3%n" +
                "restartFirst=200%n" +
                "restartInc=0.8%n" +
                "clauseDecay=0.92%n" +
                "lbLBDMinimizingClause=3%n" +
                "lbLBDFrozenClause=25%n" +
                "lbSizeMinimizingClause=24%n" +
                "firstReduceDB=1999%n" +
                "specialIncReduceDB=999%n" +
                "incReduceDB=299%n" +
                "factorK=0.7%n" +
                "factorR=1.3%n" +
                "sizeLBDQueue=45%n" +
                "sizeTrailQueue=4999%n" +
                "reduceOnSize=true%n" +
                "reduceOnSizeSize=10%n" +
                "maxVarDecay=0.99%n" +
                "}");
        assertThat(config.toString()).isEqualTo(expected);
    }

    @Test
    public void testMaxSATConfigToString() {
        final MaxSATConfig config = MaxSATConfig.builder()
                .incremental(MaxSATConfig.IncrementalStrategy.ITERATIVE)
                .cardinality(MaxSATConfig.CardinalityEncoding.MTOTALIZER)
                .weight(MaxSATConfig.WeightStrategy.DIVERSIFY)
                .verbosity(MaxSATConfig.Verbosity.SOME)
                .output(System.out)
                .symmetry(false)
                .limit(1000)
                .bmo(false)
                .build();
        final String expected = String.format("MaxSATConfig{%n" +
                "incrementalStrategy=ITERATIVE%n" +
                "pbEncoding=LADDER%n" +
                "pbEncoding=SWC%n" +
                "cardinalityEncoding=MTOTALIZER%n" +
                "weightStrategy=DIVERSIFY%n" +
                "verbosity=SOME%n" +
                "symmetry=false%n" +
                "limit=1000%n" +
                "bmo=false%n" +
                "}");
        assertThat(config.toString()).isEqualTo(expected);
    }

    @Test
    public void testFormulaRandomizerConfigToString() {
        final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                .seed(42)
                .numVars(21)
                .variables(FormulaFactory.caching().variables("a", "b", "c"))
                .build();
        final String expected = String.format("FormulaRandomizerConfig{%n" +
                "seed=42%n" +
                "variables=[a, b, c]%n" +
                "numVars=21%n" +
                "weightConstant=0.1%n" +
                "weightPositiveLiteral=1.0%n" +
                "weightNegativeLiteral=1.0%n" +
                "weightOr=30.0%n" +
                "weightAnd=30.0%n" +
                "weightNot=1.0%n" +
                "weightImpl=1.0%n" +
                "weightEquiv=1.0%n" +
                "maximumOperandsAnd=5%n" +
                "maximumOperandsOr=5%n" +
                "weightPbc=0.0%n" +
                "weightPbcCoeffPositive=1.0%n" +
                "weightPbcCoeffNegative=0.2%n" +
                "weightPbcTypeLe=0.2%n" +
                "weightPbcTypeLt=0.2%n" +
                "weightPbcTypeGe=0.2%n" +
                "weightPbcTypeGt=0.2%n" +
                "weightPbcTypeEq=0.2%n" +
                "maximumOperandsPbc=5%n" +
                "maximumCoefficientPbc=10%n" +
                "weightCc=0.0%n" +
                "weightAmo=0.0%n" +
                "weightExo=0.0%n" +
                "maximumOperandsCc=5%n" +
                "}"
        );
        assertThat(config.toString()).isEqualTo(expected);
    }
}
