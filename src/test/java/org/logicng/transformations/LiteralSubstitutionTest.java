// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.io.parsers.ParserException;

import java.util.HashMap;
import java.util.Map;

public class LiteralSubstitutionTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private LiteralSubstitution s1;

    @BeforeEach
    public void init() {
        final Map<Literal, Literal> map = new HashMap<>();
        map.put(this.f.literal("a", true), this.f.literal("a_t", true));
        map.put(this.f.literal("a", false), this.f.literal("a_f", true));
        map.put(this.f.literal("b", false), this.f.literal("x", true));
        map.put(this.f.literal("c", true), this.f.literal("y", true));
        this.s1 = new LiteralSubstitution(f, map);
    }

    @Test
    public void testSimpleFormula() throws ParserException {
        assertThat(this.f.parse("$true").transform(this.s1)).isEqualTo(this.f.parse("$true"));
        assertThat(this.f.parse("$false").transform(this.s1)).isEqualTo(this.f.parse("$false"));
    }

    @Test
    public void testLiterals() throws ParserException {
        assertThat(this.f.parse("m").transform(this.s1)).isEqualTo(this.f.parse("m"));
        assertThat(this.f.parse("~m").transform(this.s1)).isEqualTo(this.f.parse("~m"));
        assertThat(this.f.parse("a").transform(this.s1)).isEqualTo(this.f.parse("a_t"));
        assertThat(this.f.parse("~a").transform(this.s1)).isEqualTo(this.f.parse("a_f"));
        assertThat(this.f.parse("b").transform(this.s1)).isEqualTo(this.f.parse("b"));
        assertThat(this.f.parse("~b").transform(this.s1)).isEqualTo(this.f.parse("x"));
        assertThat(this.f.parse("c").transform(this.s1)).isEqualTo(this.f.parse("y"));
        assertThat(this.f.parse("~c").transform(this.s1)).isEqualTo(this.f.parse("~y"));
    }

    @Test
    public void testFormulas() throws ParserException {
        assertThat(this.f.parse("~(a & b & ~c & x)").transform(this.s1)).isEqualTo(this.f.parse("~(a_t & b & ~y & x)"));
        assertThat(this.f.parse("a & b & ~c & x").transform(this.s1)).isEqualTo(this.f.parse("a_t & b & ~y & x"));
        assertThat(this.f.parse("a | b | ~c | x").transform(this.s1)).isEqualTo(this.f.parse("a_t | b | ~y | x"));
        assertThat(this.f.parse("(a | b) => (~c | x)").transform(this.s1)).isEqualTo(this.f.parse("(a_t | b) => (~y | x)"));
        assertThat(this.f.parse("(a | b) <=> (~c | x)").transform(this.s1)).isEqualTo(this.f.parse("(a_t | b) <=> (~y | x)"));
        assertThat(this.f.parse("2*a + 3*~b + -4*~c + x <= 5").transform(this.s1)).isEqualTo(this.f.parse("2*a_t + 3*x + -4*~y + x <= 5"));
    }

    @Test
    public void testEmptySubstitution() throws ParserException {
        assertThat(this.f.parse("2*a + 3*~b + -4*~c + x <= 5").transform(new LiteralSubstitution(f, Map.of())))
                .isEqualTo(this.f.parse("2*a + 3*~b + -4*~c + x <= 5"));
    }

}
