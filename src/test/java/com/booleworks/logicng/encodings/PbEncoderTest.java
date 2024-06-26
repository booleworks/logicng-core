// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PbEncoderTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final List<EncoderConfig> configs;

    public PbEncoderTest() {
        configs = Arrays.asList(
                EncoderConfig.builder().pbEncoding(EncoderConfig.PB_ENCODER.SWC).build(),
                EncoderConfig.builder()
                        .pbEncoding(EncoderConfig.PB_ENCODER.BINARY_MERGE)
                        .binaryMergeUseGAC(false)
                        .amoEncoding(EncoderConfig.AMO_ENCODER.NESTED)
                        .build(),
                null
        );
    }

    @Test
    public void testCC0() {
        for (final EncoderConfig config : configs) {
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
            final List<Formula> clauses = PbEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 0, lits, coeffs), config);
            final SATSolver solver = SATSolver.newSolver(f);
            solver.add(clauses);
            assertSolverSat(solver);
            Assertions.assertThat(solver.enumerateAllModels(problemLits))
                    .hasSize(1)
                    .allMatch(m -> m.positiveVariables().isEmpty());
        }
    }

    @Test
    public void testCC1() {
        for (final EncoderConfig config : configs) {
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
            final List<Formula> clauses = PbEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, rhs, lits, coeffs),
                    config);
            final SATSolver solver = SATSolver.newSolver(f);
            solver.add(clauses);
            assertSolverSat(solver);
            Assertions.assertThat(solver.enumerateAllModels(problemLits))
                    .hasSize(numLits + 1)
                    .allMatch(m -> m.positiveVariables().size() <= rhs);
        }
    }

    @Test
    public void testCCs() {
        for (final EncoderConfig config : configs) {
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

    private void testCC(final int numLits, final int rhs, final int expected, final EncoderConfig config) {
        final Variable[] problemLits = new Variable[numLits];
        final int[] coeffs = new int[numLits];
        for (int i = 0; i < numLits; i++) {
            problemLits[i] = f.variable("v" + i);
            coeffs[i] = 1;
        }
        final List<Formula> clauses = PbEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, rhs, problemLits, coeffs),
                config);
        final SATSolver solver = SATSolver.newSolver(f);
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
        for (final EncoderConfig config : configs) {
            Assertions.assertThat(PbEncoder.encode(f, truePBC, config)).isEmpty();
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
        Assertions.assertThat(PbEncoder.encode(f, normCC, configs.get(0))).containsExactly(f.parse("~m | ~n"));
    }
}
