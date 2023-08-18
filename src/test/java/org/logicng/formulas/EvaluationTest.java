// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Assignment;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;

public class EvaluationTest extends TestWithExampleFormulas {

    private final Assignment ass = new Assignment(Arrays.asList(A, B, C, NX, NY));

    @Test
    public void testConstantEval() {
        assertThat(TRUE.evaluate(ass)).isTrue();
        assertThat(FALSE.evaluate(ass)).isFalse();
    }

    @Test
    public void testLiteralEval() {
        assertThat(A.evaluate(ass)).isTrue();
        assertThat(NA.evaluate(ass)).isFalse();
        assertThat(X.evaluate(ass)).isFalse();
        assertThat(NX.evaluate(ass)).isTrue();
    }

    @Test
    public void testNotEval() {
        assertThat(NOT1.evaluate(ass)).isFalse();
        assertThat(NOT2.evaluate(ass)).isTrue();
    }

    @Test
    public void testBinaryEval() {
        assertThat(IMP1.evaluate(ass)).isTrue();
        assertThat(IMP2.evaluate(ass)).isTrue();
        assertThat(IMP3.evaluate(ass)).isFalse();
        assertThat(IMP4.evaluate(ass)).isTrue();

        assertThat(EQ1.evaluate(ass)).isTrue();
        assertThat(EQ2.evaluate(ass)).isTrue();
        assertThat(EQ3.evaluate(ass)).isFalse();
        assertThat(EQ4.evaluate(ass)).isTrue();
    }

    @Test
    public void testNAryEval() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(OR1.evaluate(ass)).isFalse();
        assertThat(OR2.evaluate(ass)).isTrue();
        assertThat(OR3.evaluate(ass)).isTrue();
        assertThat(p.parse("~a | ~b | ~c | x | y").evaluate(ass)).isFalse();
        assertThat(p.parse("~a | ~b | ~c | x | ~y").evaluate(ass)).isTrue();

        assertThat(AND1.evaluate(ass)).isTrue();
        assertThat(AND2.evaluate(ass)).isFalse();
        assertThat(AND3.evaluate(ass)).isFalse();
        assertThat(p.parse("a & b & c & ~x & ~y").evaluate(ass)).isTrue();
        assertThat(p.parse("a & b & c & ~x & y").evaluate(ass)).isFalse();
    }
}
