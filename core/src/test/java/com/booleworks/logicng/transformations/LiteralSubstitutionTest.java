// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

public class LiteralSubstitutionTest extends TestWithFormulaContext {

    public LiteralSubstitution ls(final FormulaFactory f) {
        final Map<Literal, Literal> map = new HashMap<>();
        map.put(f.literal("a", true), f.literal("a_t", true));
        map.put(f.literal("a", false), f.literal("a_f", true));
        map.put(f.literal("b", false), f.literal("x", true));
        map.put(f.literal("c", true), f.literal("y", true));
        return new LiteralSubstitution(f, map);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleFormula(final FormulaContext _c) throws ParserException {
        final LiteralSubstitution s1 = ls(_c.f);

        assertThat(_c.f.parse("$true").transform(s1)).isEqualTo(_c.f.parse("$true"));
        assertThat(_c.f.parse("$false").transform(s1)).isEqualTo(_c.f.parse("$false"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) throws ParserException {
        final LiteralSubstitution s1 = ls(_c.f);

        assertThat(_c.f.parse("m").transform(s1)).isEqualTo(_c.f.parse("m"));
        assertThat(_c.f.parse("~m").transform(s1)).isEqualTo(_c.f.parse("~m"));
        assertThat(_c.f.parse("a").transform(s1)).isEqualTo(_c.f.parse("a_t"));
        assertThat(_c.f.parse("~a").transform(s1)).isEqualTo(_c.f.parse("a_f"));
        assertThat(_c.f.parse("b").transform(s1)).isEqualTo(_c.f.parse("b"));
        assertThat(_c.f.parse("~b").transform(s1)).isEqualTo(_c.f.parse("x"));
        assertThat(_c.f.parse("c").transform(s1)).isEqualTo(_c.f.parse("y"));
        assertThat(_c.f.parse("~c").transform(s1)).isEqualTo(_c.f.parse("~y"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulas(final FormulaContext _c) throws ParserException {
        final LiteralSubstitution s1 = ls(_c.f);

        assertThat(_c.f.parse("~(a & b & ~c & x)").transform(s1)).isEqualTo(_c.f.parse("~(a_t & b & ~y & x)"));
        assertThat(_c.f.parse("a & b & ~c & x").transform(s1)).isEqualTo(_c.f.parse("a_t & b & ~y & x"));
        assertThat(_c.f.parse("a | b | ~c | x").transform(s1)).isEqualTo(_c.f.parse("a_t | b | ~y | x"));
        assertThat(_c.f.parse("(a | b) => (~c | x)").transform(s1)).isEqualTo(_c.f.parse("(a_t | b) => (~y | x)"));
        assertThat(_c.f.parse("(a | b) <=> (~c | x)").transform(s1)).isEqualTo(_c.f.parse("(a_t | b) <=> (~y | x)"));
        assertThat(_c.f.parse("2*a + 3*~b + -4*~c + x <= 5").transform(s1))
                .isEqualTo(_c.f.parse("2*a_t + 3*x + -4*~y + x <= 5"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEmptySubstitution(final FormulaContext _c) throws ParserException {
        assertThat(_c.f.parse("2*a + 3*~b + -4*~c + x <= 5").transform(new LiteralSubstitution(_c.f, Map.of())))
                .isEqualTo(_c.f.parse("2*a + 3*~b + -4*~c + x <= 5"));
    }
}
