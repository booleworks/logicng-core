// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.formulas.cache.TransformationCacheEntry;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.predicates.AIGPredicate;

public class AIGTest extends TestWithExampleFormulas {

    private final AIGTransformation aigCaching = new AIGTransformation(f);
    private final AIGTransformation aigNonCaching = new AIGTransformation(f, false);
    private final AIGPredicate aigPred = new AIGPredicate();

    @Test
    public void testConstants() {
        assertThat(this.TRUE.transform(this.aigCaching)).isEqualTo(this.TRUE);
        assertThat(this.FALSE.transform(this.aigCaching)).isEqualTo(this.FALSE);
        assertThat(this.TRUE.holds(this.aigPred)).isTrue();
        assertThat(this.FALSE.holds(this.aigPred)).isTrue();
    }

    @Test
    public void testLiterals() {
        assertThat(this.A.transform(this.aigCaching)).isEqualTo(this.A);
        assertThat(this.NA.transform(this.aigCaching)).isEqualTo(this.NA);
        assertThat(this.A.holds(this.aigPred)).isTrue();
        assertThat(this.NA.holds(this.aigPred)).isTrue();
    }

    @Test
    public void testBinaryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(this.f);
        assertThat(this.IMP1.transform(this.aigCaching)).isEqualTo(p.parse("~(a & ~b)"));
        assertThat(this.IMP2.transform(this.aigCaching)).isEqualTo(p.parse("~(~a & b)"));
        assertThat(this.IMP3.transform(this.aigCaching)).isEqualTo(p.parse("~((a & b) & (~x & ~y))"));
        assertThat(this.EQ1.transform(this.aigCaching)).isEqualTo(p.parse("~(a & ~b) & ~(~a & b)"));
        assertThat(this.EQ2.transform(this.aigCaching)).isEqualTo(p.parse("~(a & ~b) & ~(~a & b)"));
        assertThat(this.IMP1.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.IMP2.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.IMP3.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.EQ1.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.EQ2.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.IMP1.holds(this.aigPred)).isFalse();
        assertThat(this.IMP2.holds(this.aigPred)).isFalse();
        assertThat(this.IMP3.holds(this.aigPred)).isFalse();
        assertThat(this.EQ1.holds(this.aigPred)).isFalse();
        assertThat(this.EQ2.holds(this.aigPred)).isFalse();
        final Formula impl = p.parse("m => n");
        impl.transform(this.aigNonCaching);
        final Formula aigIMPL = impl.transformationCacheEntry(TransformationCacheEntry.AIG);
        assertThat(aigIMPL).isNull();
        final Formula equi = p.parse("m <=> n");
        equi.transform(this.aigNonCaching);
        final Formula aigEQUI = impl.transformationCacheEntry(TransformationCacheEntry.AIG);
        assertThat(aigEQUI).isNull();
    }

    @Test
    public void testNAryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(this.f);
        assertThat(this.AND1.transform(this.aigCaching)).isEqualTo(this.AND1);
        assertThat(this.OR1.transform(this.aigCaching)).isEqualTo(p.parse("~(~x & ~y)"));
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(this.aigCaching)).isEqualTo(p.parse("(~a & ~b) & c & ~(x & ~y) & ~(w & ~z)"));
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(this.aigCaching)).isEqualTo(p.parse("~(a & b & ~c & ~(~x & y))"));
        assertThat(p.parse("a | b | (~x & ~y)").transform(this.aigCaching)).isEqualTo(p.parse("~(~a & ~b & ~(~x & ~y))"));
        assertThat(this.AND1.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.OR1.transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(p.parse("a | b | (~x & ~y)").transform(this.aigCaching).holds(this.aigPred)).isTrue();
        assertThat(this.AND1.holds(this.aigPred)).isTrue();
        assertThat(this.f.and(this.AND1, this.PBC1).holds(this.aigPred)).isFalse();
        assertThat(this.OR1.holds(this.aigPred)).isFalse();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").holds(this.aigPred)).isFalse();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").holds(this.aigPred)).isFalse();
        assertThat(p.parse("a | b | (~x & ~y)").holds(this.aigPred)).isFalse();
        final Formula or = p.parse("m | n | o");
        or.transform(this.aigNonCaching);
        final Formula aigOR = or.transformationCacheEntry(TransformationCacheEntry.AIG);
        assertThat(aigOR).isNull();
        final Formula and = p.parse("m & n & o");
        and.transform(this.aigNonCaching);
        final Formula aigAND = and.transformationCacheEntry(TransformationCacheEntry.AIG);
        assertThat(aigAND).isNull();
    }

    @Test
    public void testNot() throws ParserException {
        final PropositionalParser p = new PropositionalParser(this.f);
        assertThat(p.parse("~a").transform(this.aigCaching)).isEqualTo(p.parse("~a"));
        assertThat(p.parse("~~a").transform(this.aigCaching)).isEqualTo(p.parse("a"));
        assertThat(p.parse("~(a => b)").transform(this.aigCaching)).isEqualTo(p.parse("a & ~b"));
        assertThat(p.parse("~(~(a | b) => ~(x | y))").transform(this.aigCaching)).isEqualTo(p.parse("(~a & ~b) & ~(~x & ~y)"));
        assertThat(p.parse("~(a <=> b)").transform(this.aigCaching)).isEqualTo(p.parse("~(~(a & ~b) & ~(~a & b))"));
        assertThat(p.parse("~(~(a | b) <=> ~(x | y))").transform(this.aigCaching)).isEqualTo(p.parse("~(~(~a & ~b & ~(~x & ~y)) & ~((a | b) & ~(x | y)))"));
        assertThat(p.parse("~(a & b & ~x & ~y)").transform(this.aigCaching)).isEqualTo(p.parse("~(a & b & ~x & ~y)"));
        assertThat(p.parse("~(a | b | ~x | ~y)").transform(this.aigCaching)).isEqualTo(p.parse("~a & ~b & x & y"));
        assertThat(p.parse("~(a | b | ~x | ~y)").transform(this.aigCaching)).isEqualTo(p.parse("~a & ~b & x & y")); // test caching
        final Formula not = p.parse("~(m | n)");
        not.transform(this.aigNonCaching);
        final Formula aig = not.transformationCacheEntry(TransformationCacheEntry.AIG);
        assertThat(aig).isNull();
    }

    @Test
    public void testPBC() {
        assertThat(this.PBC1.transform(this.aigCaching).holds(this.aigPred)).isTrue();
    }
}
