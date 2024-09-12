// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MaxSATClassTest {

    @Test
    public void testMaxSATConfig() {
        assertThat(Arrays.asList(MaxSATConfig.IncrementalStrategy.values())
                .contains(MaxSATConfig.IncrementalStrategy.valueOf("ITERATIVE"))).isTrue();
        assertThat(
                Arrays.asList(MaxSATConfig.AMOEncoding.values()).contains(MaxSATConfig.AMOEncoding.valueOf("LADDER")))
                        .isTrue();
        assertThat(Arrays.asList(MaxSATConfig.PBEncoding.values()).contains(MaxSATConfig.PBEncoding.valueOf("SWC")))
                .isTrue();
        assertThat(Arrays.asList(MaxSATConfig.CardinalityEncoding.values())
                .contains(MaxSATConfig.CardinalityEncoding.valueOf("TOTALIZER"))).isTrue();
        assertThat(Arrays.asList(MaxSATConfig.WeightStrategy.values())
                .contains(MaxSATConfig.WeightStrategy.valueOf("DIVERSIFY"))).isTrue();
        assertThat(Arrays.asList(MaxSATConfig.Verbosity.values()).contains(MaxSATConfig.Verbosity.valueOf("SOME")))
                .isTrue();
    }
}
