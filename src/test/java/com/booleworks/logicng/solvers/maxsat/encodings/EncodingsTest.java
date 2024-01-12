// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.encodings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.sat.MiniSat2Solver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncodingsTest {

    @Test
    public void testTotalizer() {
        final Totalizer totalizer = new Totalizer(MaxSATConfig.IncrementalStrategy.ITERATIVE);
        Assertions.assertThat(totalizer.incremental()).isEqualTo(MaxSATConfig.IncrementalStrategy.ITERATIVE);
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
            swc.encode(new MiniSat2Solver(FormulaFactory.caching()), new LNGIntVector(), new LNGIntVector(), Integer.MAX_VALUE);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Overflow in the encoding.");
        assertThatThrownBy(() -> {
            final SequentialWeightCounter swc = new SequentialWeightCounter();
            swc.encode(new MiniSat2Solver(FormulaFactory.caching()), new LNGIntVector(), new LNGIntVector(), Integer.MAX_VALUE, new LNGIntVector(), 1);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Overflow in the encoding.");
    }

    @Test
    public void testSequentialWeightCounter() {
        final SequentialWeightCounter swc = new SequentialWeightCounter();
        assertThat(swc.hasCreatedEncoding()).isEqualTo(false);
    }
}
