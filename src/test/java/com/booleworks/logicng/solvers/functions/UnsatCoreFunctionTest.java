// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import org.junit.jupiter.api.Test;

public class UnsatCoreFunctionTest extends TestWithExampleFormulas {

    @Test
    public void testExceptionalBehavior() {
        assertThatThrownBy(() -> {
            final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(false).build());
            solver.execute(UnsatCoreFunction.get());
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot generate an unsat core if proof generation is not turned on");
        assertThatThrownBy(() -> {
            final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).build());
            solver.sat();
            solver.execute(UnsatCoreFunction.get());
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("An unsat core can only be generated if the formula is solved and is UNSAT");
        assertThatThrownBy(() -> {
            final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).build());
            solver.execute(UnsatCoreFunction.get());
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot generate an unsat core before the formula was solved.");
        assertThatThrownBy(() -> {
            final SATSolver solver = MiniSat.miniCard(f, MiniSatConfig.builder().proofGeneration(true).build());
            solver.add(f.parse("A & (A => B) & (B => ~A)"));
            solver.sat();
            solver.execute(UnsatCoreFunction.get());
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot compute an unsat core with MiniCard.");
        assertThatThrownBy(() -> {
            final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).build());
            solver.add(f.variable("A"));
            solver.sat(f.literal("A", false));
            solver.execute(UnsatCoreFunction.get());
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot compute an unsat core for a computation with assumptions.");
    }
}
