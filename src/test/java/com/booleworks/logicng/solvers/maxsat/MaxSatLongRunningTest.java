// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static com.booleworks.logicng.solvers.maxsat.MaxSATReader.readCnfToSolver;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MaxSatLongRunningTest {

    @Test
    @LongRunningTag
    public void testWeightedMaxSat() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final File folder = new File("src/test/resources/longrunning/wms");
        final Map<String, Integer> result = readResult(new File("src/test/resources/longrunning/wms/result.txt"));
        final MaxSATSolver[] solvers = new MaxSATSolver[3];
        solvers[0] = MaxSATSolver.oll(f);
        solvers[1] = MaxSATSolver.incWBO(f, MaxSATConfig.builder()
                .cnfMethod(SATSolverConfig.CNFMethod.FACTORY_CNF).weight(MaxSATConfig.WeightStrategy.DIVERSIFY).build());
        solvers[2] = MaxSATSolver.incWBO(f, MaxSATConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.FACTORY_CNF).build());
        for (final MaxSATSolver solver : solvers) {
            for (final File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.getName().endsWith("wcnf")) {
                    solver.reset();
                    readCnfToSolver(solver, file.getAbsolutePath());
                    assertThat(solver.solve().getOptimum()).isEqualTo(result.get(file.getName()));
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
