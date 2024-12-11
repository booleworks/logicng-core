// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatastructuresTest {

    @Test
    public void testTristate() {
        assertThat(Tristate.valueOf("TRUE")).isEqualTo(Tristate.TRUE);
        assertThat(Tristate.valueOf("FALSE")).isEqualTo(Tristate.FALSE);
        assertThat(Tristate.valueOf("UNDEF")).isEqualTo(Tristate.UNDEF);
    }
}
