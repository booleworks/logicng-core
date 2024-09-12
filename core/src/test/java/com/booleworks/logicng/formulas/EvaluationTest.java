// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

public class EvaluationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstantEval(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.b, _c.c, _c.nx, _c.ny));
        assertThat(_c.verum.evaluate(ass)).isTrue();
        assertThat(_c.falsum.evaluate(ass)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiteralEval(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.b, _c.c, _c.nx, _c.ny));
        assertThat(_c.a.evaluate(ass)).isTrue();
        assertThat(_c.na.evaluate(ass)).isFalse();
        assertThat(_c.x.evaluate(ass)).isFalse();
        assertThat(_c.nx.evaluate(ass)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotEval(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.b, _c.c, _c.nx, _c.ny));
        assertThat(_c.not1.evaluate(ass)).isFalse();
        assertThat(_c.not2.evaluate(ass)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryEval(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.b, _c.c, _c.nx, _c.ny));
        assertThat(_c.imp1.evaluate(ass)).isTrue();
        assertThat(_c.imp2.evaluate(ass)).isTrue();
        assertThat(_c.imp3.evaluate(ass)).isFalse();
        assertThat(_c.imp4.evaluate(ass)).isTrue();

        assertThat(_c.eq1.evaluate(ass)).isTrue();
        assertThat(_c.eq2.evaluate(ass)).isTrue();
        assertThat(_c.eq3.evaluate(ass)).isFalse();
        assertThat(_c.eq4.evaluate(ass)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryEval(final FormulaContext _c) throws ParserException {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.b, _c.c, _c.nx, _c.ny));
        final PropositionalParser p = new PropositionalParser(_c.f);
        assertThat(_c.or1.evaluate(ass)).isFalse();
        assertThat(_c.or2.evaluate(ass)).isTrue();
        assertThat(_c.or3.evaluate(ass)).isTrue();
        assertThat(p.parse("~a | ~b | ~c | x | y").evaluate(ass)).isFalse();
        assertThat(p.parse("~a | ~b | ~c | x | ~y").evaluate(ass)).isTrue();

        assertThat(_c.and1.evaluate(ass)).isTrue();
        assertThat(_c.and2.evaluate(ass)).isFalse();
        assertThat(_c.and3.evaluate(ass)).isFalse();
        assertThat(p.parse("a & b & c & ~x & ~y").evaluate(ass)).isTrue();
        assertThat(p.parse("a & b & c & ~x & y").evaluate(ass)).isFalse();
    }
}
