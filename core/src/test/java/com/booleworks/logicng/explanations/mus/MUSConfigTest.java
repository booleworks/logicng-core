// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.transformations.cnf.CNFConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MUSConfigTest {

    @Test
    public void testMUSConfiguration() {
        final MUSConfig config = MUSConfig.builder().algorithm(MUSConfig.Algorithm.valueOf("DELETION")).build();
        assertThat(config.toString()).isEqualTo(String.format("MUSConfig{%nalgorithm=DELETION%n}%n"));
        assertThat(Arrays.asList(CNFConfig.Algorithm.values()))
                .contains(CNFConfig.Algorithm.valueOf("TSEITIN"));
    }
}
