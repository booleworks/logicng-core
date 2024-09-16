// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;

public class UnsatCoreFunctionTest extends TestWithExampleFormulas {

    @Test
    public void testExceptionalBehavior() {
        assertThatThrownBy(() -> {
            final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(false).build());
            solver.satCall().unsatCore();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot generate an unsat core if proof generation is not turned on");

        assertThat(
                SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build()).satCall().unsatCore())
                .isNull();
    }
}
