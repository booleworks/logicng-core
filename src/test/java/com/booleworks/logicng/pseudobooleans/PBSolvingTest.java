// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PBSolvingTest implements LogicNGTest {

    private final FormulaFactory f;
    private final Variable[] literals100;
    private final Variable[] literals10;
    private final List<SATSolver> solvers;
    private final List<Supplier<SATSolver>> solverSuppliers;

    private final PBConfig[] configs;

    public PBSolvingTest() {
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
        solverSuppliers = SolverTestSet.solverSupplierTestSet(Set.of(USE_AT_MOST_CLAUSES), f);
        configs = new PBConfig[]{
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.SWC).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(true).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(false).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(true).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(false).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(true).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(false).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(true).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(false).build(),
                PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.ADDER_NETWORKS).build(),
        };
    }

    @Test
    public void testCCAMO() {
        for (final SATSolver solver : solvers) {
            solver.add(f.amo(literals100));
            final List<Model> models = solver.enumerateAllModels(literals100);
            assertThat(models.size()).isEqualTo(101);
            for (final Model model : models) {
                assertThat(model.positiveVariables().size() <= 1).isTrue();
            }
        }
    }

    @Test
    public void testCCEXO() {
        for (final SATSolver solver : solvers) {
            solver.add(f.exo(literals100));
            final List<Model> models = solver.enumerateAllModels(literals100);
            assertThat(models.size()).isEqualTo(100);
            for (final Model model : models) {
                assertThat(model.positiveVariables().size() == 1).isTrue();
            }
        }
    }

    @Test
    public void testCCAMK() {
        for (final Supplier<SATSolver> solver : solverSuppliers) {
            testCCAMK(solver.get(), 0, 1);
            testCCAMK(solver.get(), 1, 11);
            testCCAMK(solver.get(), 2, 56);
            testCCAMK(solver.get(), 3, 176);
            testCCAMK(solver.get(), 4, 386);
            testCCAMK(solver.get(), 5, 638);
            testCCAMK(solver.get(), 6, 848);
            testCCAMK(solver.get(), 7, 968);
            testCCAMK(solver.get(), 8, 1013);
            testCCAMK(solver.get(), 9, 1023);
        }
    }

    @Test
    public void testCCLT() {
        for (final Supplier<SATSolver> solver : solverSuppliers) {
            testCCLT(solver.get(), 1, 1);
            testCCLT(solver.get(), 2, 11);
            testCCLT(solver.get(), 3, 56);
            testCCLT(solver.get(), 4, 176);
            testCCLT(solver.get(), 5, 386);
            testCCLT(solver.get(), 6, 638);
            testCCLT(solver.get(), 7, 848);
            testCCLT(solver.get(), 8, 968);
            testCCLT(solver.get(), 9, 1013);
            testCCLT(solver.get(), 10, 1023);
        }
    }

    @Test
    public void testCCALK() {
        for (final Supplier<SATSolver> solver : solverSuppliers) {
            testCCALK(solver.get(), 1, 1023);
            testCCALK(solver.get(), 2, 1013);
            testCCALK(solver.get(), 3, 968);
            testCCALK(solver.get(), 4, 848);
            testCCALK(solver.get(), 5, 638);
            testCCALK(solver.get(), 6, 386);
            testCCALK(solver.get(), 7, 176);
            testCCALK(solver.get(), 8, 56);
            testCCALK(solver.get(), 9, 11);
            testCCALK(solver.get(), 10, 1);
        }
    }

    @Test
    public void testCCGT() {
        for (final Supplier<SATSolver> solver : solverSuppliers) {
            testCCGT(solver.get(), 0, 1023);
            testCCGT(solver.get(), 1, 1013);
            testCCGT(solver.get(), 2, 968);
            testCCGT(solver.get(), 3, 848);
            testCCGT(solver.get(), 4, 638);
            testCCGT(solver.get(), 5, 386);
            testCCGT(solver.get(), 6, 176);
            testCCGT(solver.get(), 7, 56);
            testCCGT(solver.get(), 8, 11);
            testCCGT(solver.get(), 9, 1);
        }
    }

    @Test
    public void testCCEQ() {
        for (final Supplier<SATSolver> solver : solverSuppliers) {
            testCCEQ(solver.get(), 0, 1);
            testCCEQ(solver.get(), 1, 10);
            testCCEQ(solver.get(), 2, 45);
            testCCEQ(solver.get(), 3, 120);
            testCCEQ(solver.get(), 4, 210);
            testCCEQ(solver.get(), 5, 252);
            testCCEQ(solver.get(), 6, 210);
            testCCEQ(solver.get(), 7, 120);
            testCCEQ(solver.get(), 8, 45);
            testCCEQ(solver.get(), 9, 10);
            testCCEQ(solver.get(), 10, 1);
        }
    }

    private void testCCAMK(final SATSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.LE, rhs, literals10));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() <= rhs);
    }

    private void testCCLT(final SATSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.LT, rhs, literals10));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() < rhs);
    }

    private void testCCALK(final SATSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.GE, rhs, literals10));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() >= rhs);
    }

    private void testCCGT(final SATSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.GT, rhs, literals10));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() > rhs);
    }

    private void testCCEQ(final SATSolver solver, final int rhs, final int expected) {
        solver.add(f.cc(CType.EQ, rhs, literals10));
        assertSolverSat(solver);
        Assertions.assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() == rhs);
    }

    @Test
    public void testPBEQ() {
        for (final PBConfig config : configs) {
            for (final Supplier<SATSolver> solverSupplier : solverSuppliers) {
                SATSolver solver = solverSupplier.get();
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 5, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(9)
                        .allMatch(model -> model.positiveVariables().size() == 2)
                        .allMatch(model -> model.positiveVariables().contains(f.variable("v" + 0)));
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 7, literals10, coeffs10), config));
                assertSolverSat(solver);

                Assertions.assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(36)
                        .allMatch(model -> model.positiveVariables().size() == 3)
                        .allMatch(model -> model.positiveVariables().contains(f.variable("v" + 0)));
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 0, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 1, literals10, coeffs10), config));
                assertSolverUnsat(solver);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 22, literals10, coeffs10), config));
                assertSolverUnsat(solver);
            }
        }
    }

    @Test
    public void testPBLess() {
        for (final PBConfig config : configs) {
            for (final Supplier<SATSolver> solverSupplier : solverSuppliers) {
                SATSolver solver = solverSupplier.get();
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 6, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(140)
                        .allMatch(model -> model.positiveVariables().size() <= 3);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LT, 7, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(140)
                        .allMatch(model -> model.positiveVariables().size() <= 3);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 0, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 1, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LT, 2, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LT, 1, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
            }
        }
    }

    @Test
    public void testPBGreater() {
        for (final PBConfig config : configs) {
            for (final Supplier<SATSolver> solverSupplier : solverSuppliers) {
                SATSolver solver = solverSupplier.get();
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GE, 17, literals10, coeffs10), config));
                assertSolverSat(solver);

                Assertions.assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(47)
                        .allMatch(model -> model.positiveVariables().size() >= 8);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GT, 16, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(47)
                        .allMatch(model -> model.positiveVariables().size() >= 8);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GE, 21, literals10, coeffs10), config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GE, 22, literals10, coeffs10), config));
                assertSolverUnsat(solver);
                solver = solverSupplier.get();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GT, 42, literals10, coeffs10), config));
                assertSolverUnsat(solver);
            }
        }
    }

    @Test
    public void testPBNegative() {
        for (final PBConfig config : configs) {
            for (final Supplier<SATSolver> solverSupplier : solverSuppliers) {
                SATSolver solver = solverSupplier.get();
                int[] coeffs10 = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, -2};
                final PBConstraint pbc = (PBConstraint) f.pbc(CType.EQ, 2, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc, config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(45).allMatch(m -> pbc.evaluate(m.assignment()));
                solver = solverSupplier.get();

                coeffs10 = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, -2};
                final PBConstraint pbc2 = (PBConstraint) f.pbc(CType.EQ, 4, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc2, config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(120).allMatch(m -> pbc2.evaluate(m.assignment()));
                solver = solverSupplier.get();

                coeffs10 = new int[]{2, 2, -3, 2, -7, 2, 2, 2, 2, -2};
                final PBConstraint pbc3 = (PBConstraint) f.pbc(CType.EQ, 4, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc3, config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(57).allMatch(m -> pbc3.evaluate(m.assignment()));
                solver = solverSupplier.get();

                coeffs10 = new int[]{2, 2, -3, 2, -7, 2, 2, 2, 2, -2};
                final PBConstraint pbc4 = (PBConstraint) f.pbc(CType.EQ, -10, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc4, config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(8).allMatch(m -> pbc4.evaluate(m.assignment()));
                solver = solverSupplier.get();

                coeffs10 = new int[]{2, 2, -4, 2, -6, 2, 2, 2, 2, -2};
                final PBConstraint pbc5 = (PBConstraint) f.pbc(CType.EQ, -12, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc5, config));
                assertSolverSat(solver);
                Assertions.assertThat(solver.enumerateAllModels(literals10)).hasSize(1).allMatch(m -> pbc5.evaluate(m.assignment()));
            }
        }
    }

    @Test
    @LongRunningTag
    public void testLargePBs() {
        for (final PBConfig config : configs) {
            for (final Supplier<SATSolver> solverSupplier : solverSuppliers) {
                final SATSolver solver = solverSupplier.get();
                final int numLits = 100;
                final Variable[] lits = new Variable[numLits];
                final int[] coeffs = new int[numLits];
                for (int i = 0; i < numLits; i++) {
                    lits[i] = f.variable("v" + i);
                    coeffs[i] = i + 1;
                }
                final PBConstraint pbc = (PBConstraint) f.pbc(CType.GE, 5000, lits, coeffs);
                solver.add(PBEncoder.encode(f, pbc, config));
                assertSolverSat(solver);
            assertThat(pbc.evaluate(solver.satCall().model(Arrays.asList(lits)))).isTrue();
            }
        }
    }
}
