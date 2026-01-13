// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class PbSolvingTest implements LogicNGTest {

    private final FormulaFactory f;
    private final Variable[] literals100;
    private final Variable[] literals10;
    private final List<SatSolver> solvers;
    private final List<Function<FormulaFactory, SatSolver>> solverSuppliers;

    private final EncoderConfig[] configs;

    public PbSolvingTest() {
        f = FormulaFactory.caching();
        literals100 = new Variable[100];
        literals10 = new Variable[10];
        for (int i = 0; i < 100; i++) {
            literals100[i] = f.variable("v" + i);
        }
        for (int i = 0; i < 10; i++) {
            literals10[i] = f.variable("v" + i);
        }
        solvers = SolverTestSet.solverTestSet(Set.of(USE_AT_MOST_CLAUSES), f);
        solverSuppliers = SolverTestSet.solverSupplierTestSet(Set.of(USE_AT_MOST_CLAUSES));
        configs = new EncoderConfig[]{
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.SWC).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(true)
                        .binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(true).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(true)
                        .binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(false).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(true)
                        .binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(true).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(true)
                        .binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(false).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(false)
                        .binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(true).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(false)
                        .binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(false).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(false)
                        .binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(true).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.BINARY_MERGE).binaryMergeUseGac(false)
                        .binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(false).build(),
                EncoderConfig.builder().pbEncoding(EncoderConfig.PbEncoder.ADDER_NETWORKS).build(),
        };
    }

    @Test
    public void testCCAMO() {
        for (final SatSolver solver : solvers) {
            solver.add(f.amo(literals100));
            final List<Model> models = solver.enumerateAllModels(List.of(literals100));
            assertThat(models.size()).isEqualTo(101);
            for (final Model model : models) {
                assertThat(model.positiveVariables().size() <= 1).isTrue();
            }
        }
    }

    @Test
    public void testCCEXO() {
        for (final SatSolver solver : solvers) {
            solver.add(f.exo(literals100));
            final List<Model> models = solver.enumerateAllModels(List.of(literals100));
            assertThat(models.size()).isEqualTo(100);
            for (final Model model : models) {
                assertThat(model.positiveVariables().size() == 1).isTrue();
            }
        }
    }

    @Test
    public void testCCAMK() {
        for (final Function<FormulaFactory, SatSolver> solver : solverSuppliers) {
            testCCAMK(solver.apply(f), 0, 1);
            testCCAMK(solver.apply(f), 1, 11);
            testCCAMK(solver.apply(f), 2, 56);
            testCCAMK(solver.apply(f), 3, 176);
            testCCAMK(solver.apply(f), 4, 386);
            testCCAMK(solver.apply(f), 5, 638);
            testCCAMK(solver.apply(f), 6, 848);
            testCCAMK(solver.apply(f), 7, 968);
            testCCAMK(solver.apply(f), 8, 1013);
            testCCAMK(solver.apply(f), 9, 1023);
        }
    }

    @Test
    public void testCCLT() {
        for (final Function<FormulaFactory, SatSolver> solver : solverSuppliers) {
            testCCLT(solver.apply(f), 1, 1);
            testCCLT(solver.apply(f), 2, 11);
            testCCLT(solver.apply(f), 3, 56);
            testCCLT(solver.apply(f), 4, 176);
            testCCLT(solver.apply(f), 5, 386);
            testCCLT(solver.apply(f), 6, 638);
            testCCLT(solver.apply(f), 7, 848);
            testCCLT(solver.apply(f), 8, 968);
            testCCLT(solver.apply(f), 9, 1013);
            testCCLT(solver.apply(f), 10, 1023);
        }
    }

    @Test
    public void testCCALK() {
        for (final Function<FormulaFactory, SatSolver> solver : solverSuppliers) {
            testCCALK(solver.apply(f), 1, 1023);
            testCCALK(solver.apply(f), 2, 1013);
            testCCALK(solver.apply(f), 3, 968);
            testCCALK(solver.apply(f), 4, 848);
            testCCALK(solver.apply(f), 5, 638);
            testCCALK(solver.apply(f), 6, 386);
            testCCALK(solver.apply(f), 7, 176);
            testCCALK(solver.apply(f), 8, 56);
            testCCALK(solver.apply(f), 9, 11);
            testCCALK(solver.apply(f), 10, 1);
        }
    }

    @Test
    public void testCCGT() {
        for (final Function<FormulaFactory, SatSolver> solver : solverSuppliers) {
            testCCGT(solver.apply(f), 0, 1023);
            testCCGT(solver.apply(f), 1, 1013);
            testCCGT(solver.apply(f), 2, 968);
            testCCGT(solver.apply(f), 3, 848);
            testCCGT(solver.apply(f), 4, 638);
            testCCGT(solver.apply(f), 5, 386);
            testCCGT(solver.apply(f), 6, 176);
            testCCGT(solver.apply(f), 7, 56);
            testCCGT(solver.apply(f), 8, 11);
            testCCGT(solver.apply(f), 9, 1);
        }
    }

    @Test
    public void testCCEQ() {
        for (final Function<FormulaFactory, SatSolver> solver : solverSuppliers) {
            testCCEQ(solver.apply(f), 0, 1);
            testCCEQ(solver.apply(f), 1, 10);
            testCCEQ(solver.apply(f), 2, 45);
            testCCEQ(solver.apply(f), 3, 120);
            testCCEQ(solver.apply(f), 4, 210);
            testCCEQ(solver.apply(f), 5, 252);
            testCCEQ(solver.apply(f), 6, 210);
            testCCEQ(solver.apply(f), 7, 120);
            testCCEQ(solver.apply(f), 8, 45);
            testCCEQ(solver.apply(f), 9, 10);
            testCCEQ(solver.apply(f), 10, 1);
        }
    }

    private void testCCAMK(final SatSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.LE, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(List.of(literals10)))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() <= rhs);
    }

    private void testCCLT(final SatSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.LT, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(List.of(literals10)))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() < rhs);
    }

    private void testCCALK(final SatSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.GE, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(List.of(literals10)))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() >= rhs);
    }

    private void testCCGT(final SatSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.GT, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(List.of(literals10)))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() > rhs);
    }

    private void testCCEQ(final SatSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.EQ, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(List.of(literals10)))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() == rhs);
    }

    @Test
    public void testPBEQ() {
        for (final EncoderConfig config : configs) {
            for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
                SatSolver solver = solverSupplier.apply(f);
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.EQ, 5, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10)))
                        .hasSize(9)
                        .allMatch(model -> model.positiveVariables().size() == 2)
                        .allMatch(model -> model.positiveVariables().contains(f.variable("v" + 0)));
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.EQ, 7, literals10, coeffs10), config));
                assertSolverSat(solver);

                assertThat(solver.enumerateAllModels(List.of(literals10)))
                        .hasSize(36)
                        .allMatch(model -> model.positiveVariables().size() == 3)
                        .allMatch(model -> model.positiveVariables().contains(f.variable("v" + 0)));
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.EQ, 0, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.EQ, 1, literals10, coeffs10), config));
                assertSolverUnsat(solver);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.EQ, 22, literals10, coeffs10), config));
                assertSolverUnsat(solver);
            }
        }
    }

    @Test
    public void testPBLess() {
        for (final EncoderConfig config : configs) {
            for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
                SatSolver solver = solverSupplier.apply(f);
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.LE, 6, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10)))
                        .hasSize(140)
                        .allMatch(model -> model.positiveVariables().size() <= 3);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.LT, 7, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10)))
                        .hasSize(140)
                        .allMatch(model -> model.positiveVariables().size() <= 3);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.LE, 0, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.LE, 1, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.LT, 2, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.LT, 1, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1);
            }
        }
    }

    @Test
    public void testPBGreater() {
        for (final EncoderConfig config : configs) {
            for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
                SatSolver solver = solverSupplier.apply(f);
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.GE, 17, literals10, coeffs10), config));
                assertSolverSat(solver);

                assertThat(solver.enumerateAllModels(List.of(literals10)))
                        .hasSize(47)
                        .allMatch(model -> model.positiveVariables().size() >= 8);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.GT, 16, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10)))
                        .hasSize(47)
                        .allMatch(model -> model.positiveVariables().size() >= 8);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.GE, 21, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.GE, 22, literals10, coeffs10), config));
                assertSolverUnsat(solver);
                solver = solverSupplier.apply(f);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f,
                        (PbConstraint) f.pbc(CType.GT, 42, literals10, coeffs10), config));
                assertSolverUnsat(solver);
            }
        }
    }

    @Test
    public void testPBNegative() {
        for (final EncoderConfig config : configs) {
            for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
                SatSolver solver = solverSupplier.apply(f);
                int[] coeffs10 = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, -2};
                final PbConstraint pbc = (PbConstraint) f.pbc(CType.EQ, 2, literals10, coeffs10);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f, pbc, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(45)
                        .allMatch(m -> pbc.evaluate(m.toAssignment()));
                solver = solverSupplier.apply(f);

                coeffs10 = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, -2};
                final PbConstraint pbc2 = (PbConstraint) f.pbc(CType.EQ, 4, literals10, coeffs10);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f, pbc2, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(120)
                        .allMatch(m -> pbc2.evaluate(m.toAssignment()));
                solver = solverSupplier.apply(f);

                coeffs10 = new int[]{2, 2, -3, 2, -7, 2, 2, 2, 2, -2};
                final PbConstraint pbc3 = (PbConstraint) f.pbc(CType.EQ, 4, literals10, coeffs10);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f, pbc3, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(57)
                        .allMatch(m -> pbc3.evaluate(m.toAssignment()));
                solver = solverSupplier.apply(f);

                coeffs10 = new int[]{2, 2, -3, 2, -7, 2, 2, 2, 2, -2};
                final PbConstraint pbc4 = (PbConstraint) f.pbc(CType.EQ, -10, literals10, coeffs10);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f, pbc4, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(8)
                        .allMatch(m -> pbc4.evaluate(m.toAssignment()));
                solver = solverSupplier.apply(f);

                coeffs10 = new int[]{2, 2, -4, 2, -6, 2, 2, 2, 2, -2};
                final PbConstraint pbc5 = (PbConstraint) f.pbc(CType.EQ, -12, literals10, coeffs10);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f, pbc5, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(List.of(literals10))).hasSize(1)
                        .allMatch(m -> pbc5.evaluate(m.toAssignment()));
            }
        }
    }

    @Test
    @LongRunningTag
    public void testLargePBs() {
        for (final EncoderConfig config : configs) {
            for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
                final SatSolver solver = solverSupplier.apply(f);
                final int numLits = 100;
                final Variable[] lits = new Variable[numLits];
                final int[] coeffs = new int[numLits];
                for (int i = 0; i < numLits; i++) {
                    lits[i] = f.variable("v" + i);
                    coeffs[i] = i + 1;
                }
                final PbConstraint pbc = (PbConstraint) f.pbc(CType.GE, 5000, lits, coeffs);
                solver.add(com.booleworks.logicng.encodings.PbEncoder.encode(f, pbc, config));
                assertSolverSat(solver);
                assertThat(pbc.evaluate(solver.satCall().model(Arrays.asList(lits)).toAssignment())).isTrue();
            }
        }
    }
}
