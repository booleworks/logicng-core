//  SPDX-License-Identifier: Apache-2.0 and MIT
//  Copyright 2015-2023 Christoph Zengler
//  Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.encodingresult;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Literal;
import org.junit.jupiter.api.Test;

public class EncodingAuxiliaryVariableTest {

    @Test
    public void testEncodingAuxiliaryVariable() {
        final EncodingAuxiliaryVariable eav = new EncodingAuxiliaryVariable("var");
        assertThat(eav.toString()).isEqualTo("var");
        final Literal negated1 = eav.negate(null);
        assertThat(negated1.toString()).isEqualTo("~var");
        assertThat(eav.equals(negated1)).isFalse();
        final Literal eav2 = new EncodingAuxiliaryLiteral("var", false);
        final Literal negated2 = new EncodingAuxiliaryLiteral("var", true);
        assertThat(eav.equals(eav2)).isTrue();
        assertThat(negated1.equals(negated2)).isTrue();
    }
}
