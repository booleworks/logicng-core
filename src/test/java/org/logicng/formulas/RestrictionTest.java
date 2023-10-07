// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.datastructures.Assignment;
import org.logicng.io.parsers.ParserException;

import java.util.Arrays;

public class RestrictionTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstantRestrict(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nb, _c.nx));
        assertThat(_c.verum.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.falsum.restrict(_c.f, ass)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiteralRestrict(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nb, _c.nx));
        assertThat(_c.a.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.na.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.x.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.nx.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.c.restrict(_c.f, ass)).isEqualTo(_c.c);
        assertThat(_c.ny.restrict(_c.f, ass)).isEqualTo(_c.ny);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotRestrict(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nb, _c.nx));
        assertThat(_c.not1.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.not2.restrict(_c.f, ass)).isEqualTo(_c.ny);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryRestrict(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nb, _c.nx));
        assertThat(_c.imp1.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.imp2.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.f.implication(_c.na, _c.c).restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.imp3.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.f.implication(_c.a, _c.c).restrict(_c.f, ass)).isEqualTo(_c.c);

        assertThat(_c.eq1.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.eq2.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.eq3.restrict(_c.f, ass)).isEqualTo(_c.ny);
        assertThat(_c.eq4.restrict(_c.f, ass)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryRestrict(final FormulaContext _c) throws ParserException {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nb, _c.nx));
        assertThat(_c.or1.restrict(_c.f, ass)).isEqualTo(_c.y);
        assertThat(_c.or2.restrict(_c.f, ass)).isEqualTo(_c.verum);
        assertThat(_c.or3.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.p.parse("~a | b | ~c | x | y").restrict(_c.f, ass)).isEqualTo(_c.p.parse("~c | y"));
        assertThat(_c.p.parse("~a | b | ~c | ~x | ~y").restrict(_c.f, ass)).isEqualTo(_c.verum);

        assertThat(_c.and1.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.and2.restrict(_c.f, ass)).isEqualTo(_c.falsum);
        assertThat(_c.and3.restrict(_c.f, ass)).isEqualTo(_c.y);
        assertThat(_c.p.parse("a & ~b & c & ~x & ~y").restrict(_c.f, ass)).isEqualTo(_c.p.parse("c & ~y"));
        assertThat(_c.p.parse("a & b & c & ~x & y").restrict(_c.f, ass)).isEqualTo(_c.falsum);
    }
}
