// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;

public class LngHeapTest {

    @Test
    public void test() {
        final LngCoreSolver solver = new LngCoreSolver(FormulaFactory.caching(), SatSolverConfig.builder().build());
        solver.newVar(true, true);
        solver.newVar(true, true);
        solver.newVar(true, true);
        final LngHeap heap = new LngHeap(solver);
        assertThat(heap.empty()).isTrue();
        heap.insert(1);
        heap.insert(2);
        heap.insert(0);
        assertThat(heap.get(0)).isEqualTo(1);
        assertThat(heap.toString()).isEqualTo("LngHeap{[1, 2], [2, 0], [0, 1]}");
        assertThat(heap.size()).isEqualTo(3);
        heap.clear();
        assertThat(heap.empty()).isTrue();
    }
}
