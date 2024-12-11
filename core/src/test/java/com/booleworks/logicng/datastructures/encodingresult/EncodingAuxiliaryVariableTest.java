package com.booleworks.logicng.datastructures.encodingresult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EncodingAuxiliaryVariableTest {

    @Test
    public void testEncodingAuxiliaryVariable() {
        final EncodingAuxiliaryVariable eav = new EncodingAuxiliaryVariable("var", false);
        assertThat(eav.toString()).isEqualTo("var");
    }
}
