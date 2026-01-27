// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.MaxSATReader.readCnfToSolver;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class MaxSatLongRunningTest {

    @Test
    @LongRunningTag
    public void testWeightedMaxSat() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final File folder = new File("../test_files/longrunning/wms");
        final Map<String, Integer> result = readResult(new File("../test_files/longrunning/wms/result.txt"));
        final List<Supplier<MaxSatSolver>> solvers = Arrays.asList(
                () -> MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL),
                () -> MaxSatSolver.newSolver(f,
                        MaxSatConfig.builder()
                                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                                .cnfMethod(SatSolverConfig.CnfMethod.FACTORY_CNF)
                                .weight(MaxSatConfig.WeightStrategy.DIVERSIFY).build()),
                () -> MaxSatSolver.newSolver(f,
                        MaxSatConfig.builder()
                                .algorithm(MaxSatConfig.Algorithm.INC_WBO)
                                .cnfMethod(SatSolverConfig.CnfMethod.FACTORY_CNF)
                                .build())
        );
        for (final Supplier<MaxSatSolver> solverGenerator : solvers) {
            for (final File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.getName().endsWith("wcnf")) {
                    final MaxSatSolver solver = solverGenerator.get();
                    readCnfToSolver(solver, file.getAbsolutePath());
                    assertThat(solver.solve().getUnsatisfiedWeight()).isEqualTo(result.get(file.getName()));
                }
            }
        }
    }

    private Map<String, Integer> readResult(final File file) throws IOException {
        final Map<String, Integer> result = new HashMap<>();
        final List<String> lines = Files.readAllLines(file.toPath());
        for (final String line : lines) {
            final String[] tokens = line.split(";");
            result.put(tokens[0], Integer.parseInt(tokens[1]));
        }
        return result;
    }
}
