// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.cardinalityconstraints.CCConfig;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.util.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PBEncoderTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final List<Pair<PBConfig, CCConfig>> configs;

    public PBEncoderTest() {
        configs = new ArrayList<>();
        configs.add(new Pair<>(PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.SWC).build(), null));
        configs.add(new Pair<>(PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).build(),
                CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.NESTED).build()));
        configs.add(new Pair<>(null, null));
    }

    @Test
    public void testCC0() {
        for (final Pair<PBConfig, CCConfig> config : configs) {
            final int numLits = 100;
            final List<Literal> lits = new ArrayList<>();
            final List<Integer> coeffs = new ArrayList<>();
            final Variable[] problemLits = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                final Variable var = f.variable("v" + i);
                lits.add(var);
                problemLits[i] = var;
                coeffs.add(1);
            }
            final List<Formula> clauses = PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 0, lits, coeffs), config.first(), config.second());
            final SATSolver solver = SATSolver.miniSat(f);
            solver.add(clauses);
            assertSolverSat(solver);
            Assertions.assertThat(solver.enumerateAllModels(problemLits))
                    .hasSize(1)
                    .allMatch(m -> m.positiveVariables().isEmpty());
        }
    }

    @Test
    public void testCC1() {
        for (final Pair<PBConfig, CCConfig> config : configs) {
            final int numLits = 100;
            final int rhs = 1;
            final List<Literal> lits = new ArrayList<>();
            final List<Integer> coeffs = new ArrayList<>();
            final Variable[] problemLits = new Variable[numLits];
            for (int i = 0; i < numLits; i++) {
                final Variable var = f.variable("v" + i);
                lits.add(var);
                problemLits[i] = var;
                coeffs.add(1);
            }
            final List<Formula> clauses = PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, rhs, lits, coeffs), config.first(), config.second());
            final SATSolver solver = SATSolver.miniSat(f);
            solver.add(clauses);
            assertSolverSat(solver);
            Assertions.assertThat(solver.enumerateAllModels(problemLits))
                    .hasSize(numLits + 1)
                    .allMatch(m -> m.positiveVariables().size() <= rhs);
        }
    }

    @Test
    public void testCCs() {
        for (final Pair<PBConfig, CCConfig> config : configs) {
            testCC(10, 0, 1, config);
            testCC(10, 1, 11, config);
            testCC(10, 2, 56, config);
            testCC(10, 3, 176, config);
            testCC(10, 4, 386, config);
            testCC(10, 5, 638, config);
            testCC(10, 6, 848, config);
            testCC(10, 7, 968, config);
            testCC(10, 8, 1013, config);
            testCC(10, 9, 1023, config);
        }
    }

    private void testCC(final int numLits, final int rhs, final int expected, final Pair<PBConfig, CCConfig> config) {
        final Variable[] problemLits = new Variable[numLits];
        final int[] coeffs = new int[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
            coeffs[i] = 1;
        }
        final List<Formula> clauses = PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, rhs, problemLits, coeffs), config.first(), config.second());
        final SATSolver solver = SATSolver.miniSat(f);
        solver.add(clauses);
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(problemLits))
                .hasSize(expected)
                .allMatch(m -> m.positiveVariables().size() <= rhs);
    }

    @Test
    public void testSpecialCases() {
        final List<Literal> lits = new ArrayList<>();
        lits.add(f.literal("m", true));
        lits.add(f.literal("n", true));
        final List<Integer> coeffs = new ArrayList<>();
        coeffs.add(2);
        coeffs.add(1);
        final PBConstraint truePBC = (PBConstraint) f.pbc(CType.GE, 0, lits, coeffs);
        for (final Pair<PBConfig, CCConfig> config : configs) {
            Assertions.assertThat(PBEncoder.encode(f, truePBC, config.first(), config.second())).isEmpty();
        }
    }

    @Test
    public void testCCNormalized() throws ParserException {
        final List<Literal> lits = new ArrayList<>();
        lits.add(f.literal("m", true));
        lits.add(f.literal("n", true));
        final List<Integer> coeffs2 = new ArrayList<>();
        coeffs2.add(2);
        coeffs2.add(2);
        final PBConstraint normCC = (PBConstraint) f.pbc(CType.LE, 2, lits, coeffs2);
        Assertions.assertThat(PBEncoder.encode(f, normCC, configs.get(0).first(), configs.get(0).second())).containsExactly(f.parse("~m | ~n"));
    }

    @Test
    public void testConfigToString() {
        assertThat(configs.get(0).first().toString()).isEqualTo(String.format("PBConfig{%n" +
                "pbEncoder=SWC%n" +
                "binaryMergeUseGAC=true%n" +
                "binaryMergeNoSupportForSingleBit=false%n" +
                "binaryMergeUseWatchDog=true%n" +
                "}%n"));
        assertThat(Arrays.asList(PBConfig.PB_ENCODER.values()).contains(PBConfig.PB_ENCODER.valueOf("ADDER_NETWORKS"))).isTrue();
    }
}
