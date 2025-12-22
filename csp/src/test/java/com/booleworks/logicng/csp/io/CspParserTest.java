// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.io.parsers.CspParser;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CspParserTest {
    final FormulaFactory f = FormulaFactory.caching();
    final CspFactory cf = new CspFactory(f);

    @Test
    public void testExceptions() throws ParserException {
        final CspParser p = new CspParser(cf);
        assertThatThrownBy(() -> p.parsePredicate("")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parseFormula("[]")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parsePredicate((String) null)).isInstanceOf(ParserException.class);
    }

    @Test
    public void testSimplePredicates() throws ParserException {
        final CspParser p = new CspParser(cf);
        final IntegerVariable a = cf.variable("a", 0, 10);
        assertThat(p.parsePredicate("a=0")).isEqualTo(cf.eq(a, cf.zero()));
        assertThat(p.parsePredicate("EQ[a,0]")).isEqualTo(cf.eq(a, cf.zero()));
        assertThat(p.parsePredicate("a<=0")).isEqualTo(cf.le(a, cf.zero()));
        assertThat(p.parsePredicate("LE[a,0]")).isEqualTo(cf.le(a, cf.zero()));
        assertThat(p.parsePredicate("a<0")).isEqualTo(cf.lt(a, cf.zero()));
        assertThat(p.parsePredicate("LT[a,0]")).isEqualTo(cf.lt(a, cf.zero()));
        assertThat(p.parsePredicate("a>0")).isEqualTo(cf.gt(a, cf.zero()));
        assertThat(p.parsePredicate("GT[a,0]")).isEqualTo(cf.gt(a, cf.zero()));
        assertThat(p.parsePredicate("a>=0")).isEqualTo(cf.ge(a, cf.zero()));
        assertThat(p.parsePredicate("GE[a,0]")).isEqualTo(cf.ge(a, cf.zero()));
        assertThat(p.parsePredicate("ALLDIFFERENT[a,0]")).isEqualTo(cf.allDifferent(List.of(a, cf.zero())));
    }

    @Test
    public void testSimpleTerms() throws ParserException {
        final CspParser p = new CspParser(cf);
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerVariable c = cf.variable("c", 0, 10);
        assertThat(p.parsePredicate("0 = a + b + c")).isEqualTo(cf.eq(cf.zero(), cf.add(a, b, c)));
        assertThat(p.parsePredicate("0 = add(a,b,c)")).isEqualTo(cf.eq(cf.zero(), cf.add(a, b, c)));
        assertThat(p.parsePredicate("0 = add()")).isEqualTo(cf.eq(cf.zero(), cf.add()));
        assertThat(p.parsePredicate("0 = add(a)")).isEqualTo(cf.eq(cf.zero(), cf.add(a)));
        assertThat(p.parsePredicate("0 = a - b")).isEqualTo(cf.eq(cf.zero(), cf.sub(a, b)));
        assertThat(p.parsePredicate("0 = sub(a,b)")).isEqualTo(cf.eq(cf.zero(), cf.sub(a, b)));
        assertThat(p.parsePredicate("0 = a - b - c")).isEqualTo(cf.eq(cf.zero(), cf.sub(cf.sub(a, b), c)));
        assertThat(p.parsePredicate("0 = - a")).isEqualTo(cf.eq(cf.zero(), cf.minus(a)));
        assertThat(p.parsePredicate("0 = neg(a)")).isEqualTo(cf.eq(cf.zero(), cf.minus(a)));
        assertThat(p.parsePredicate("0 = a * b")).isEqualTo(cf.eq(cf.zero(), cf.mul(a, b)));
        assertThat(p.parsePredicate("0 = mul(a,b)")).isEqualTo(cf.eq(cf.zero(), cf.mul(a, b)));
        assertThat(p.parsePredicate("0 = a * b * c")).isEqualTo(cf.eq(cf.zero(), cf.mul(cf.mul(a, b), c)));
        assertThat(p.parsePredicate("0 = a / 2")).isEqualTo(cf.eq(cf.zero(), cf.div(a, 2)));
        assertThat(p.parsePredicate("0 = div(a,2)")).isEqualTo(cf.eq(cf.zero(), cf.div(a, 2)));
        assertThat(p.parsePredicate("0 = a / 2 / 5")).isEqualTo(cf.eq(cf.zero(), cf.div(cf.div(a, 2), 5)));
        assertThat(p.parsePredicate("0 = a % 2")).isEqualTo(cf.eq(cf.zero(), cf.mod(a, 2)));
        assertThat(p.parsePredicate("0 = mod(a,2)")).isEqualTo(cf.eq(cf.zero(), cf.mod(a, 2)));
        assertThat(p.parsePredicate("0 = a % 2 % 5")).isEqualTo(cf.eq(cf.zero(), cf.mod(cf.mod(a, 2), 5)));
        assertThat(p.parsePredicate("0 = min(a,b)")).isEqualTo(cf.eq(cf.zero(), cf.min(a, b)));
        assertThat(p.parsePredicate("0 = max(a,b)")).isEqualTo(cf.eq(cf.zero(), cf.max(a, b)));
        assertThat(p.parsePredicate("0 = abs(a)")).isEqualTo(cf.eq(cf.zero(), cf.abs(a)));
    }

    @Test
    public void testFormulas() throws ParserException {
        final CspParser p = new CspParser(cf);
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerVariable c = cf.variable("c", 0, 10);
        assertThat(p.parseFormula("A & [a + b = c]")).isSameAs(f.and(f.variable("A"), cf.eq(cf.add(a, b), c)));
        assertThat(p.parseFormula("A & EQ[a + b, c]")).isSameAs(f.and(f.variable("A"), cf.eq(cf.add(a, b), c)));
        assertThat(p.parseFormula("A <=> ( B => [a + b = c])"))
                .isSameAs(f.equivalence(f.variable("A"), f.implication(f.variable("B"), cf.eq(cf.add(a, b), c))));
    }

    @Test
    public void testPrecedence() throws ParserException {
        final CspParser p = new CspParser(cf);
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerVariable c = cf.variable("c", 0, 10);
        final IntegerVariable d = cf.variable("d", 0, 10);
        final IntegerVariable e = cf.variable("e", 0, 10);
        final IntegerVariable f = cf.variable("f", 0, 10);
        assertThat(p.parsePredicate("0 = a * 2 + b / 2")).isEqualTo(
                cf.eq(cf.zero(), cf.add(cf.mul(a, cf.constant(2)), cf.div(b, 2))));
        assertThat(p.parsePredicate("0 = a % 2 + b * abs(f) + (c - d)")).isEqualTo(
                cf.eq(cf.zero(), cf.add(cf.mod(a, cf.constant(2)), cf.mul(b, cf.abs(f)), cf.sub(c, d))));
        assertThat(p.parsePredicate("0 = a % 2 - b * abs(f) - (c + d)")).isEqualTo(
                cf.eq(cf.zero(), cf.sub(cf.sub(cf.mod(a, cf.constant(2)), cf.mul(b, cf.abs(f))), cf.add(c, d))));
    }

    @Test
    public void testIdentCollision() throws ParserException {
        final CspParser p = new CspParser(cf);
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerVariable min = cf.variable("min", 0, 10);
        final IntegerVariable max = cf.variable("max", 0, 10);
        final IntegerVariable eq = cf.variable("EQ", 0, 10);
        assertThat(p.parsePredicate("min = max + max(a, b) + a")).isEqualTo(cf.eq(min, cf.add(max, cf.max(a, b), a)));
        assertThat(p.parsePredicate("EQ[EQ, abs(1)]")).isEqualTo(cf.eq(eq, cf.abs(cf.one())));
    }

    @Test
    public void illegalPredicates() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final CspParser p = new CspParser(cf);
        assertThatThrownBy(() -> p.parsePredicate("[a = 0]")).isInstanceOf(ParserException.class);
        cf.variable("a", 0, 10);
        cf.variable("b", 0, 10);
        cf.variable("c", 0, 10);
        assertThatThrownBy(() -> p.parsePredicate("sub(a, b, c) = 0")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parsePredicate("sub(a) = 0")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parsePredicate("a / b = 0")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parsePredicate("a % b = 0")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parsePredicate("fun(a, b) = 0")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parsePredicate("a + b - c = 0")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parseFormula("A & []")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parseFormula("A & EQ[]")).isInstanceOf(ParserException.class);
        assertThatThrownBy(() -> p.parseFormula("A & EQ[a]")).isInstanceOf(ParserException.class);
    }
}
