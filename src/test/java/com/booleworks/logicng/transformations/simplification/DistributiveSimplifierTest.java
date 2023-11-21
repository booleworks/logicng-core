// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DistributiveSimplifierTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final DistributiveSimplifier distributiveSimplifier = new DistributiveSimplifier(_c.f);

        assertThat(_c.verum.transform(distributiveSimplifier)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(distributiveSimplifier)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final DistributiveSimplifier distributiveSimplifier = new DistributiveSimplifier(_c.f);

        assertThat(_c.a.transform(distributiveSimplifier)).isEqualTo(_c.a);
        assertThat(_c.na.transform(distributiveSimplifier)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNoPropagation(final FormulaContext _c) {
        final DistributiveSimplifier distributiveSimplifier = new DistributiveSimplifier(_c.f);

        assertThat(_c.and1.transform(distributiveSimplifier)).isEqualTo(_c.and1);
        assertThat(_c.and2.transform(distributiveSimplifier)).isEqualTo(_c.and2);
        assertThat(_c.or1.transform(distributiveSimplifier)).isEqualTo(_c.or1);
        assertThat(_c.or2.transform(distributiveSimplifier)).isEqualTo(_c.or2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPropagations(final FormulaContext _c) throws ParserException {
        final DistributiveSimplifier distributiveSimplifier = new DistributiveSimplifier(_c.f);

        assertThat(_c.f.and(_c.and1, _c.a).transform(distributiveSimplifier)).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.and2, _c.a).transform(distributiveSimplifier)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.or1, _c.x).transform(distributiveSimplifier)).isEqualTo(_c.f.and(_c.or1, _c.x));
        assertThat(_c.f.and(_c.or2, _c.x).transform(distributiveSimplifier)).isEqualTo(_c.f.and(_c.or2, _c.x));
        assertThat(_c.f.or(_c.and1, _c.a).transform(distributiveSimplifier)).isEqualTo(_c.f.or(_c.and1, _c.a));
        assertThat(_c.f.or(_c.and2, _c.a).transform(distributiveSimplifier)).isEqualTo(_c.f.or(_c.and2, _c.a));
        assertThat(_c.f.or(_c.or1, _c.x).transform(distributiveSimplifier)).isEqualTo(_c.or1);
        Assertions.assertThat(_c.p.parse("(a | b | ~c) & (~a | ~d) & (~c | d) & (~b | e | ~f | g) & (e | f | g | h) & (e | ~f | ~g | h) & f & c").transform(distributiveSimplifier))
                .isEqualTo(_c.p.parse("(a | b | ~c) & (~a | ~d) & (~c | d) & f & c & (e | (~b | ~f | g) & (f | g | h) & (~f | ~g | h))"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulaTypes(final FormulaContext _c) {
        final DistributiveSimplifier distributiveSimplifier = new DistributiveSimplifier(_c.f);

        assertThat(_c.imp1.transform(distributiveSimplifier)).isEqualTo(_c.imp1);
        assertThat(_c.eq1.transform(distributiveSimplifier)).isEqualTo(_c.eq1);
        assertThat(_c.not1.transform(distributiveSimplifier)).isEqualTo(_c.not1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testComplexExamples(final FormulaContext _c) throws ParserException {
        final DistributiveSimplifier distributiveSimplifier = new DistributiveSimplifier(_c.f);

        final Formula cAnd = _c.p.parse("(a | b | ~c) & (~a | ~d) & (~c | d | b) & (~c | ~b)");
        final Formula cAndD1 = cAnd.transform(distributiveSimplifier);
        assertThat(cAndD1).isEqualTo(_c.p.parse("(~a | ~d) & (~c | (a | b) & (d | b) & ~b)"));
        assertThat(cAndD1.transform(distributiveSimplifier)).isEqualTo(_c.p.parse("(~a | ~d) & (~c | ~b & (b | a & d))"));

        assertThat(_c.f.not(cAnd).transform(distributiveSimplifier)).isEqualTo(_c.f.not(cAndD1));

        final Formula cOr = _c.p.parse("(x & y & z) | (x & y & ~z) | (x & ~y & z)");
        final Formula cOrD1 = cOr.transform(distributiveSimplifier);
        assertThat(cOrD1).isEqualTo(_c.p.parse("x & (y & z | y & ~z | ~y & z)"));
        assertThat(cOrD1.transform(distributiveSimplifier)).isEqualTo(_c.p.parse("x & (~y & z | y)"));

        assertThat(_c.f.equivalence(cOr, cAnd).transform(distributiveSimplifier)).isEqualTo(_c.f.equivalence(cOrD1, cAndD1));
        assertThat(_c.f.implication(cOr, cAnd).transform(distributiveSimplifier)).isEqualTo(_c.f.implication(cOrD1, cAndD1));
    }
}
