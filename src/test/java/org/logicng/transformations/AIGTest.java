// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.predicates.AIGPredicate;

public class AIGTest extends TestWithExampleFormulas {

    private final AIGTransformation aigCaching = new AIGTransformation(f);
    private final AIGTransformation aigNonCaching = new AIGTransformation(f, null);
    private final AIGPredicate aigPred = new AIGPredicate();

    @Test
    public void testConstants() {
        assertThat(TRUE.transform(aigCaching)).isEqualTo(TRUE);
        assertThat(FALSE.transform(aigCaching)).isEqualTo(FALSE);
        assertThat(TRUE.holds(aigPred)).isTrue();
        assertThat(FALSE.holds(aigPred)).isTrue();
    }

    @Test
    public void testLiterals() {
        assertThat(A.transform(aigCaching)).isEqualTo(A);
        assertThat(NA.transform(aigCaching)).isEqualTo(NA);
        assertThat(A.holds(aigPred)).isTrue();
        assertThat(NA.holds(aigPred)).isTrue();
    }

    @Test
    public void testBinaryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(IMP1.transform(aigCaching)).isEqualTo(p.parse("~(a & ~b)"));
        assertThat(IMP2.transform(aigCaching)).isEqualTo(p.parse("~(~a & b)"));
        assertThat(IMP3.transform(aigCaching)).isEqualTo(p.parse("~((a & b) & (~x & ~y))"));
        assertThat(EQ1.transform(aigCaching)).isEqualTo(p.parse("~(a & ~b) & ~(~a & b)"));
        assertThat(EQ2.transform(aigCaching)).isEqualTo(p.parse("~(a & ~b) & ~(~a & b)"));
        assertThat(IMP1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(IMP2.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(IMP3.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(EQ1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(EQ2.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(IMP1.holds(aigPred)).isFalse();
        assertThat(IMP2.holds(aigPred)).isFalse();
        assertThat(IMP3.holds(aigPred)).isFalse();
        assertThat(EQ1.holds(aigPred)).isFalse();
        assertThat(EQ2.holds(aigPred)).isFalse();
        final Formula impl = p.parse("m => n");
        impl.transform(aigNonCaching);
        final Formula equi = p.parse("m <=> n");
        equi.transform(aigNonCaching);
    }

    @Test
    public void testNAryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(AND1.transform(aigCaching)).isEqualTo(AND1);
        assertThat(OR1.transform(aigCaching)).isEqualTo(p.parse("~(~x & ~y)"));
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(aigCaching)).isEqualTo(p.parse("(~a & ~b) & c & ~(x & ~y) & ~(w & ~z)"));
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(aigCaching)).isEqualTo(p.parse("~(a & b & ~c & ~(~x & y))"));
        assertThat(p.parse("a | b | (~x & ~y)").transform(aigCaching)).isEqualTo(p.parse("~(~a & ~b & ~(~x & ~y))"));
        assertThat(AND1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(OR1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(p.parse("a | b | (~x & ~y)").transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(AND1.holds(aigPred)).isTrue();
        assertThat(f.and(AND1, PBC1).holds(aigPred)).isFalse();
        assertThat(OR1.holds(aigPred)).isFalse();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").holds(aigPred)).isFalse();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").holds(aigPred)).isFalse();
        assertThat(p.parse("a | b | (~x & ~y)").holds(aigPred)).isFalse();
        final Formula or = p.parse("m | n | o");
        or.transform(aigNonCaching);
        final Formula and = p.parse("m & n & o");
        and.transform(aigNonCaching);
    }

    @Test
    public void testNot() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(p.parse("~a").transform(aigCaching)).isEqualTo(p.parse("~a"));
        assertThat(p.parse("~~a").transform(aigCaching)).isEqualTo(p.parse("a"));
        assertThat(p.parse("~(a => b)").transform(aigCaching)).isEqualTo(p.parse("a & ~b"));
        assertThat(p.parse("~(~(a | b) => ~(x | y))").transform(aigCaching)).isEqualTo(p.parse("(~a & ~b) & ~(~x & ~y)"));
        assertThat(p.parse("~(a <=> b)").transform(aigCaching)).isEqualTo(p.parse("~(~(a & ~b) & ~(~a & b))"));
        assertThat(p.parse("~(~(a | b) <=> ~(x | y))").transform(aigCaching)).isEqualTo(p.parse("~(~(~a & ~b & ~(~x & ~y)) & ~((a | b) & ~(x | y)))"));
        assertThat(p.parse("~(a & b & ~x & ~y)").transform(aigCaching)).isEqualTo(p.parse("~(a & b & ~x & ~y)"));
        assertThat(p.parse("~(a | b | ~x | ~y)").transform(aigCaching)).isEqualTo(p.parse("~a & ~b & x & y"));
        assertThat(p.parse("~(a | b | ~x | ~y)").transform(aigCaching)).isEqualTo(p.parse("~a & ~b & x & y")); // test caching
        final Formula not = p.parse("~(m | n)");
        not.transform(aigNonCaching);
    }

    @Test
    public void testPBC() {
        assertThat(PBC1.transform(aigCaching).holds(aigPred)).isTrue();
    }
}
