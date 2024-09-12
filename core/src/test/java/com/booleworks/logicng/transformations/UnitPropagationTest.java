// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UnitPropagationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final UnitPropagation unitPropagation = new UnitPropagation(_c.f);

        assertThat(_c.verum.transform(unitPropagation)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(unitPropagation)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final UnitPropagation unitPropagation = new UnitPropagation(_c.f);

        assertThat(_c.a.transform(unitPropagation)).isEqualTo(_c.a);
        assertThat(_c.na.transform(unitPropagation)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNoPropagation(final FormulaContext _c) {
        final UnitPropagation unitPropagation = new UnitPropagation(_c.f);

        assertThat(_c.and1.transform(unitPropagation)).isEqualTo(_c.and1);
        assertThat(_c.and2.transform(unitPropagation)).isEqualTo(_c.and2);
        assertThat(_c.or1.transform(unitPropagation)).isEqualTo(_c.or1);
        assertThat(_c.or2.transform(unitPropagation)).isEqualTo(_c.or2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPropagations(final FormulaContext _c) throws ParserException {
        final UnitPropagation unitPropagation = new UnitPropagation(_c.f);

        assertThat(_c.f.and(_c.and1, _c.a).transform(unitPropagation)).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.and2, _c.a).transform(unitPropagation)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.or1, _c.x).transform(unitPropagation)).isEqualTo(_c.x);
        assertThat(_c.f.or(_c.and1, _c.a).transform(unitPropagation)).isEqualTo(_c.a);
        assertThat(_c.f.or(_c.or1, _c.x).transform(unitPropagation)).isEqualTo(_c.or1);
        assertThat(_c.p.parse(
                "(a | b | ~c) & (~a | ~d) & (~c | d) & (~b | e | ~f | g) & (e | f | g | h) & (e | ~f | ~g | h) & f & c")
                .transform(unitPropagation)).isEqualTo(_c.p.parse("(e | g) & (e | ~g | h) & f & c & d & ~a & b"));
    }
}
