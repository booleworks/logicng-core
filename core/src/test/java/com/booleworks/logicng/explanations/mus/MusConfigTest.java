// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.transformations.cnf.CnfConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MusConfigTest {

    @Test
    public void testMUSConfiguration() {
        final MusConfig config = MusConfig.builder().algorithm(MusConfig.Algorithm.valueOf("DELETION")).build();
        assertThat(config.toString()).isEqualTo(String.format("MusConfig{%nalgorithm=DELETION%n}%n"));
        assertThat(Arrays.asList(CnfConfig.Algorithm.values()))
                .contains(CnfConfig.Algorithm.valueOf("TSEITIN"));
    }
}
