// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class NumberOfNodesBddHandlerTest {

    @Test
    public void testInvalidBound() {
        assertThatThrownBy(() -> new NumberOfNodesBddHandler(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The bound for added nodes must be equal or greater than 0.");
    }
}
