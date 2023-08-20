// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class FormulaTest {

    @Test
    public void testFType() {
        assertThat(FType.valueOf("AND")).isEqualTo(FType.AND);
        assertThat(Arrays.asList(FType.values()).contains(FType.valueOf("PBC"))).isTrue();
        assertThat(FType.values().length).isEqualTo(9);
    }

    @Test
    public void testCType() {
        assertThat(CType.valueOf("EQ")).isEqualTo(CType.EQ);
        assertThat(CType.valueOf("LE")).isEqualTo(CType.LE);
        assertThat(Arrays.asList(CType.values()).contains(CType.valueOf("GT"))).isTrue();
        assertThat(CType.values().length).isEqualTo(5);
    }
}
