// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;

public interface LogicNGTest {
    default void assertSolverSat(final SATSolver solver) {
        Assertions.assertThat(solver.sat()).isTrue();
    }

    default void assertSolverUnsat(final SATSolver solver) {
        Assertions.assertThat(solver.sat()).isFalse();
    }
}
