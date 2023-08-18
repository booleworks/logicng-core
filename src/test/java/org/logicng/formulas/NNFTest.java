// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

public class NNFTest extends TestWithExampleFormulas {

    @Test
    public void testConstants() {
        assertThat(TRUE.nnf()).isEqualTo(TRUE);
        assertThat(FALSE.nnf()).isEqualTo(FALSE);
    }

    @Test
    public void testLiterals() {
        assertThat(A.nnf()).isEqualTo(A);
        assertThat(NA.nnf()).isEqualTo(NA);
    }

    @Test
    public void testBinaryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(IMP1.nnf()).isEqualTo(p.parse("~a | b"));
        assertThat(IMP2.nnf()).isEqualTo(p.parse("a | ~b"));
        assertThat(IMP3.nnf()).isEqualTo(p.parse("~a | ~b | x | y"));
        assertThat(IMP4.nnf()).isEqualTo(p.parse("(~a | ~b) & (a | b) | (x | ~y) & (y | ~x)"));
        assertThat(EQ1.nnf()).isEqualTo(p.parse("(~a | b) & (~b | a)"));
        assertThat(EQ2.nnf()).isEqualTo(p.parse("(a | ~b) & (b | ~a)"));
        assertThat(EQ3.nnf()).isEqualTo(p.parse("(~a | ~b | x | y) & (~x & ~y | a & b)"));
    }

    @Test
    public void testNAryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(AND1.nnf()).isEqualTo(AND1);
        assertThat(OR1.nnf()).isEqualTo(OR1);
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").nnf()).isEqualTo(p.parse("~a & ~b & c & (~x | y) & (~w | z)"));
        assertThat(p.parse("~(a & b) | c | ~(x | ~y) | (w => z)").nnf()).isEqualTo(p.parse("~a  | ~b | c | (~x & y) | (~w | z)"));
    }

    @Test
    public void testNot() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(p.parse("~a").nnf()).isEqualTo(p.parse("~a"));
        assertThat(p.parse("~~a").nnf()).isEqualTo(p.parse("a"));
        assertThat(p.parse("~(a => b)").nnf()).isEqualTo(p.parse("a & ~b"));
        assertThat(p.parse("~(~(a | b) => ~(x | y))").nnf()).isEqualTo(p.parse("~a & ~b & (x | y)"));
        assertThat(p.parse("a <=> b").nnf()).isEqualTo(p.parse("(~a | b) & (~b | a)"));
        assertThat(p.parse("~(a <=> b)").nnf()).isEqualTo(p.parse("(~a | ~b) & (a | b)"));
        assertThat(p.parse("~(~(a | b) <=> ~(x | y))").nnf()).isEqualTo(p.parse("((a | b) | (x | y)) & ((~a & ~b) | (~x & ~y))"));
        assertThat(p.parse("~(a & b & ~x & ~y)").nnf()).isEqualTo(p.parse("~a | ~b | x | y"));
        assertThat(p.parse("~(a | b | ~x | ~y)").nnf()).isEqualTo(p.parse("~a & ~b & x & y"));
        assertThat(p.parse("~(a | b | ~x | ~y)").nnf()).isEqualTo(p.parse("~a & ~b & x & y"));
    }
}
