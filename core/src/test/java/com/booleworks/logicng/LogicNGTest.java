// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.solvers.SatSolver;

public interface LogicNGTest {
    default void assertSolverSat(final SatSolver solver) {
        assertThat(solver.sat()).isTrue();
    }

    default void assertSolverUnsat(final SatSolver solver) {
        assertThat(solver.sat()).isFalse();
    }
}
