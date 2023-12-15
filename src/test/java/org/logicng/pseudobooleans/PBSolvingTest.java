// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.pseudobooleans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.LogicNGTest;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.CType;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.solvers.sat.MiniSatConfig;

import java.util.List;

public class PBSolvingTest implements LogicNGTest {

    private final FormulaFactory f;
    private final Variable[] literals100;
    private final Variable[] literals10;
    private final SATSolver[] solvers;

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
        solvers = new SATSolver[4];
        solvers[0] = MiniSat.miniSat(f);
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useAtMostClauses(false).build());
        solvers[2] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useAtMostClauses(true).build());
        solvers[3] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).useBinaryWatchers(true).useLbdFeatures(true).build());
        configs = new PBConfig[10];
        configs[0] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.SWC).build();
        configs[1] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(true).build();
        configs[2] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(false).build();
        configs[3] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(true).build();
        configs[4] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(true).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(false).build();
        configs[5] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(true).build();
        configs[6] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(true).binaryMergeUseWatchDog(false).build();
        configs[7] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(true).build();
        configs[8] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.BINARY_MERGE).binaryMergeUseGAC(false).binaryMergeNoSupportForSingleBit(false).binaryMergeUseWatchDog(false).build();
        configs[9] = PBConfig.builder().pbEncoding(PBConfig.PB_ENCODER.ADDER_NETWORKS).build();
    }

    @Test
    public void testCCAMO() {
        for (final SATSolver solver : solvers) {
            solver.reset();
            solver.add(f.amo(literals100));
            final List<Assignment> models = solver.enumerateAllModels(literals100);
            assertThat(models.size()).isEqualTo(101);
            for (final Assignment model : models) {
                assertThat(model.positiveVariables().size() <= 1).isTrue();
            }
        }
    }

    @Test
    public void testCCEXO() {
        for (final SATSolver solver : solvers) {
            solver.reset();
            solver.add(f.exo(literals100));
            final List<Assignment> models = solver.enumerateAllModels(literals100);
            assertThat(models.size()).isEqualTo(100);
            for (final Assignment model : models) {
                assertThat(model.positiveVariables().size() == 1).isTrue();
            }
        }
    }

    @Test
    public void testCCAMK() {
        for (final SATSolver solver : solvers) {
            testCCAMK(solver, 0, 1);
            testCCAMK(solver, 1, 11);
            testCCAMK(solver, 2, 56);
            testCCAMK(solver, 3, 176);
            testCCAMK(solver, 4, 386);
            testCCAMK(solver, 5, 638);
            testCCAMK(solver, 6, 848);
            testCCAMK(solver, 7, 968);
            testCCAMK(solver, 8, 1013);
            testCCAMK(solver, 9, 1023);
        }
    }

    @Test
    public void testCCLT() {
        for (final SATSolver solver : solvers) {
            testCCLT(solver, 1, 1);
            testCCLT(solver, 2, 11);
            testCCLT(solver, 3, 56);
            testCCLT(solver, 4, 176);
            testCCLT(solver, 5, 386);
            testCCLT(solver, 6, 638);
            testCCLT(solver, 7, 848);
            testCCLT(solver, 8, 968);
            testCCLT(solver, 9, 1013);
            testCCLT(solver, 10, 1023);
        }
    }

    @Test
    public void testCCALK() {
        for (final SATSolver solver : solvers) {
            testCCALK(solver, 1, 1023);
            testCCALK(solver, 2, 1013);
            testCCALK(solver, 3, 968);
            testCCALK(solver, 4, 848);
            testCCALK(solver, 5, 638);
            testCCALK(solver, 6, 386);
            testCCALK(solver, 7, 176);
            testCCALK(solver, 8, 56);
            testCCALK(solver, 9, 11);
            testCCALK(solver, 10, 1);
        }
    }

    @Test
    public void testCCGT() {
        for (final SATSolver solver : solvers) {
            testCCGT(solver, 0, 1023);
            testCCGT(solver, 1, 1013);
            testCCGT(solver, 2, 968);
            testCCGT(solver, 3, 848);
            testCCGT(solver, 4, 638);
            testCCGT(solver, 5, 386);
            testCCGT(solver, 6, 176);
            testCCGT(solver, 7, 56);
            testCCGT(solver, 8, 11);
            testCCGT(solver, 9, 1);
        }
    }

    @Test
    public void testCCEQ() {
        for (final SATSolver solver : solvers) {
            testCCEQ(solver, 0, 1);
            testCCEQ(solver, 1, 10);
            testCCEQ(solver, 2, 45);
            testCCEQ(solver, 3, 120);
            testCCEQ(solver, 4, 210);
            testCCEQ(solver, 5, 252);
            testCCEQ(solver, 6, 210);
            testCCEQ(solver, 7, 120);
            testCCEQ(solver, 8, 45);
            testCCEQ(solver, 9, 10);
            testCCEQ(solver, 10, 1);
        }
    }

    private void testCCAMK(final SATSolver solver, final int rhs, final int expected) {
        solver.reset();
        solver.add(f.cc(CType.LE, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() <= rhs);
    }

    private void testCCLT(final SATSolver solver, final int rhs, final int expected) {
        solver.reset();
        solver.add(f.cc(CType.LT, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() < rhs);
    }

    private void testCCALK(final SATSolver solver, final int rhs, final int expected) {
        solver.reset();
        solver.add(f.cc(CType.GE, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() >= rhs);
    }

    private void testCCGT(final SATSolver solver, final int rhs, final int expected) {
        solver.reset();
        solver.add(f.cc(CType.GT, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() > rhs);
    }

    private void testCCEQ(final SATSolver solver, final int rhs, final int expected) {
        solver.reset();
        solver.add(f.cc(CType.EQ, rhs, literals10));
        assertSolverSat(solver);
        assertThat(solver.enumerateAllModels(literals10))
                .hasSize(expected)
                .allMatch(model -> model.positiveVariables().size() == rhs);
    }

    @Test
    public void testPBEQ() {
        for (final PBConfig config : configs) {
            for (final SATSolver solver : solvers) {
                solver.reset();
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 5, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(9)
                        .allMatch(model -> model.positiveVariables().size() == 2)
                        .allMatch(model -> model.positiveVariables().contains(f.variable("v" + 0)));
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 7, literals10, coeffs10), config));
                assertSolverSat(solver);

                assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(36)
                        .allMatch(model -> model.positiveVariables().size() == 3)
                        .allMatch(model -> model.positiveVariables().contains(f.variable("v" + 0)));
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 0, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 1, literals10, coeffs10), config));
                assertSolverUnsat(solver);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.EQ, 22, literals10, coeffs10), config));
                assertSolverUnsat(solver);
            }
        }
    }

    @Test
    public void testPBLess() {
        for (final PBConfig config : configs) {
            for (final SATSolver solver : solvers) {
                solver.reset();
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 6, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(140)
                        .allMatch(model -> model.positiveVariables().size() <= 3);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LT, 7, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(140)
                        .allMatch(model -> model.positiveVariables().size() <= 3);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 0, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LE, 1, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LT, 2, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.LT, 1, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
            }
        }
    }

    @Test
    public void testPBGreater() {
        for (final PBConfig config : configs) {
            for (final SATSolver solver : solvers) {
                solver.reset();
                final int[] coeffs10 = new int[]{3, 2, 2, 2, 2, 2, 2, 2, 2, 2};
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GE, 17, literals10, coeffs10), config));
                assertSolverSat(solver);

                assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(47)
                        .allMatch(model -> model.positiveVariables().size() >= 8);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GT, 16, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10))
                        .hasSize(47)
                        .allMatch(model -> model.positiveVariables().size() >= 8);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GE, 21, literals10, coeffs10), config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GE, 22, literals10, coeffs10), config));
                assertSolverUnsat(solver);
                solver.reset();
                solver.add(PBEncoder.encode(f, (PBConstraint) f.pbc(CType.GT, 42, literals10, coeffs10), config));
                assertSolverUnsat(solver);
            }
        }
    }

    @Test
    public void testPBNegative() {
        for (final PBConfig config : configs) {
            for (final SATSolver solver : solvers) {
                solver.reset();
                int[] coeffs10 = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, -2};
                final PBConstraint pbc = (PBConstraint) f.pbc(CType.EQ, 2, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(45).allMatch(pbc::evaluate);
                solver.reset();

                coeffs10 = new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, -2};
                final PBConstraint pbc2 = (PBConstraint) f.pbc(CType.EQ, 4, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc2, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(120).allMatch(pbc2::evaluate);
                solver.reset();

                coeffs10 = new int[]{2, 2, -3, 2, -7, 2, 2, 2, 2, -2};
                final PBConstraint pbc3 = (PBConstraint) f.pbc(CType.EQ, 4, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc3, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(57).allMatch(pbc3::evaluate);
                solver.reset();

                coeffs10 = new int[]{2, 2, -3, 2, -7, 2, 2, 2, 2, -2};
                final PBConstraint pbc4 = (PBConstraint) f.pbc(CType.EQ, -10, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc4, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(8).allMatch(pbc4::evaluate);
                solver.reset();

                coeffs10 = new int[]{2, 2, -4, 2, -6, 2, 2, 2, 2, -2};
                final PBConstraint pbc5 = (PBConstraint) f.pbc(CType.EQ, -12, literals10, coeffs10);
                solver.add(PBEncoder.encode(f, pbc5, config));
                assertSolverSat(solver);
                assertThat(solver.enumerateAllModels(literals10)).hasSize(1).allMatch(pbc5::evaluate);
                solver.reset();
            }
        }
    }

    @Test
    public void testLargePBs() {
        for (final PBConfig config : configs) {
            final SATSolver solver = solvers[0];
            solver.reset();
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
            assertThat(pbc.evaluate(solver.model())).isTrue();
        }
    }
}
