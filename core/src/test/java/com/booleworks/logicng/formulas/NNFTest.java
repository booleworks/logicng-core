// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class NNFTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        assertThat(_c.verum.nnf(_c.f)).isEqualTo(_c.verum);
        assertThat(_c.falsum.nnf(_c.f)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.a.nnf(_c.f)).isEqualTo(_c.a);
        assertThat(_c.na.nnf(_c.f)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) throws ParserException {
        assertThat(_c.imp1.nnf(_c.f)).isEqualTo(_c.p.parse("~a | b"));
        assertThat(_c.imp2.nnf(_c.f)).isEqualTo(_c.p.parse("a | ~b"));
        assertThat(_c.imp3.nnf(_c.f)).isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.imp4.nnf(_c.f)).isEqualTo(_c.p.parse("(~a | ~b) & (a | b) | (x | ~y) & (y | ~x)"));
        assertThat(_c.eq1.nnf(_c.f)).isEqualTo(_c.p.parse("(~a | b) & (~b | a)"));
        assertThat(_c.eq2.nnf(_c.f)).isEqualTo(_c.p.parse("(a | ~b) & (b | ~a)"));
        assertThat(_c.eq3.nnf(_c.f)).isEqualTo(_c.p.parse("(~a | ~b | x | y) & (~x & ~y | a & b)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        assertThat(_c.and1.nnf(_c.f)).isEqualTo(_c.and1);
        assertThat(_c.or1.nnf(_c.f)).isEqualTo(_c.or1);
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").nnf(_c.f))
                .isEqualTo(_c.p.parse("~a & ~b & c & (~x | y) & (~w | z)"));
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y) | (w => z)").nnf(_c.f))
                .isEqualTo(_c.p.parse("~a  | ~b | c | (~x & y) | (~w | z)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        assertThat(_c.p.parse("~a").nnf(_c.f)).isEqualTo(_c.p.parse("~a"));
        assertThat(_c.p.parse("~~a").nnf(_c.f)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(a => b)").nnf(_c.f)).isEqualTo(_c.p.parse("a & ~b"));
        assertThat(_c.p.parse("~(~(a | b) => ~(x | y))").nnf(_c.f))
                .isEqualTo(_c.p.parse("~a & ~b & (x | y)"));
        assertThat(_c.p.parse("a <=> b").nnf(_c.f)).isEqualTo(_c.p.parse("(~a | b) & (~b | a)"));
        assertThat(_c.p.parse("~(a <=> b)").nnf(_c.f)).isEqualTo(_c.p.parse("(~a | ~b) & (a | b)"));
        assertThat(_c.p.parse("~(~(a | b) <=> ~(x | y))").nnf(_c.f))
                .isEqualTo(_c.p.parse("((a | b) | (x | y)) & ((~a & ~b) | (~x & ~y))"));
        assertThat(_c.p.parse("~(a & b & ~x & ~y)").nnf(_c.f)).isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.p.parse("~(a | b | ~x | ~y)").nnf(_c.f)).isEqualTo(_c.p.parse("~a & ~b & x & y"));
        assertThat(_c.p.parse("~(a | b | ~x | ~y)").nnf(_c.f)).isEqualTo(_c.p.parse("~a & ~b & x & y"));
    }
}
