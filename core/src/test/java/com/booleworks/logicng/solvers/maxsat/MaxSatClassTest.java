// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MaxSatClassTest {

    @Test
    public void testMaxSATConfig() {
        assertThat(Arrays.asList(MaxSatConfig.IncrementalStrategy.values())
                .contains(MaxSatConfig.IncrementalStrategy.valueOf("ITERATIVE"))).isTrue();
        assertThat(
                Arrays.asList(MaxSatConfig.AmoEncoding.values()).contains(MaxSatConfig.AmoEncoding.valueOf("LADDER")))
                .isTrue();
        assertThat(Arrays.asList(MaxSatConfig.PbEncoding.values()).contains(MaxSatConfig.PbEncoding.valueOf("SWC")))
                .isTrue();
        assertThat(Arrays.asList(MaxSatConfig.CardinalityEncoding.values())
                .contains(MaxSatConfig.CardinalityEncoding.valueOf("TOTALIZER"))).isTrue();
        assertThat(Arrays.asList(MaxSatConfig.WeightStrategy.values())
                .contains(MaxSatConfig.WeightStrategy.valueOf("DIVERSIFY"))).isTrue();
        assertThat(Arrays.asList(MaxSatConfig.Verbosity.values()).contains(MaxSatConfig.Verbosity.valueOf("SOME")))
                .isTrue();
    }
}
