// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.encodings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;

public class EncodingsTest {

    @Test
    public void testTotalizer() {
        final Totalizer totalizer = new Totalizer(MaxSatConfig.IncrementalStrategy.ITERATIVE);
        assertThat(totalizer.incremental()).isEqualTo(MaxSatConfig.IncrementalStrategy.ITERATIVE);
    }

    @Test
    public void testModularTotalizer() {
        final ModularTotalizer mTotalizer = new ModularTotalizer();
        assertThat(mTotalizer.hasCreatedEncoding()).isEqualTo(false);
    }

    @Test
    public void testSequentialWeightCounterExceptionalBehavior() {
        assertThatThrownBy(() -> {
            final SequentialWeightCounter swc = new SequentialWeightCounter();
            swc.encode(new LngCoreSolver(FormulaFactory.caching(), SatSolverConfig.builder().build()),
                    new LngIntVector(), new LngIntVector(), Integer.MAX_VALUE);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Overflow in the encoding.");
        assertThatThrownBy(() -> {
            final SequentialWeightCounter swc = new SequentialWeightCounter();
            swc.encode(new LngCoreSolver(FormulaFactory.caching(), SatSolverConfig.builder().build()),
                    new LngIntVector(), new LngIntVector(), Integer.MAX_VALUE, new LngIntVector(), 1);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Overflow in the encoding.");
    }

    @Test
    public void testSequentialWeightCounter() {
        final SequentialWeightCounter swc = new SequentialWeightCounter();
        assertThat(swc.hasCreatedEncoding()).isEqualTo(false);
    }
}
