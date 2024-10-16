// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.jbuddy;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.Test;

public class BddPrimeTest {

    @Test
    public void testNumberOfBits() {
        assertThat(BddPrime.numberOfBits(0)).isEqualTo(0);
        assertThat(BddPrime.numberOfBits(1)).isEqualTo(1);
    }

}
