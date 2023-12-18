// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.predicates.AIGPredicate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AIGTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final AIGPredicate aigPred = new AIGPredicate(_c.f);

        final AIGTransformation aigCaching = new AIGTransformation(_c.f);

        assertThat(_c.verum.transform(aigCaching)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(aigCaching)).isEqualTo(_c.falsum);
        assertThat(_c.verum.holds(aigPred)).isTrue();
        assertThat(_c.falsum.holds(aigPred)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final AIGPredicate aigPred = new AIGPredicate(_c.f);
        final AIGTransformation aigCaching = new AIGTransformation(_c.f);

        assertThat(_c.a.transform(aigCaching)).isEqualTo(_c.a);
        assertThat(_c.na.transform(aigCaching)).isEqualTo(_c.na);
        assertThat(_c.a.holds(aigPred)).isTrue();
        assertThat(_c.na.holds(aigPred)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) throws ParserException {
        final AIGPredicate aigPred = new AIGPredicate(_c.f);
        final AIGTransformation aigCaching = new AIGTransformation(_c.f);
        final AIGTransformation aigNonCaching = new AIGTransformation(_c.f, null);

        assertThat(_c.imp1.transform(aigCaching)).isEqualTo(_c.p.parse("~(a & ~b)"));
        assertThat(_c.imp2.transform(aigCaching)).isEqualTo(_c.p.parse("~(~a & b)"));
        assertThat(_c.imp3.transform(aigCaching)).isEqualTo(_c.p.parse("~((a & b) & (~x & ~y))"));
        assertThat(_c.eq1.transform(aigCaching)).isEqualTo(_c.p.parse("~(a & ~b) & ~(~a & b)"));
        assertThat(_c.eq2.transform(aigCaching)).isEqualTo(_c.p.parse("~(a & ~b) & ~(~a & b)"));
        assertThat(_c.imp1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.imp2.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.imp3.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.eq1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.eq2.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.imp1.holds(aigPred)).isFalse();
        assertThat(_c.imp2.holds(aigPred)).isFalse();
        assertThat(_c.imp3.holds(aigPred)).isFalse();
        assertThat(_c.eq1.holds(aigPred)).isFalse();
        assertThat(_c.eq2.holds(aigPred)).isFalse();
        final Formula impl = _c.p.parse("m => n");
        impl.transform(aigNonCaching);
        final Formula equi = _c.p.parse("m <=> n");
        equi.transform(aigNonCaching);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final AIGPredicate aigPred = new AIGPredicate(_c.f);
        final AIGTransformation aigCaching = new AIGTransformation(_c.f);
        final AIGTransformation aigNonCaching = new AIGTransformation(_c.f, null);

        assertThat(_c.and1.transform(aigCaching)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(aigCaching)).isEqualTo(_c.p.parse("~(~x & ~y)"));
        Assertions.assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(aigCaching))
                .isEqualTo(_c.p.parse("(~a & ~b) & c & ~(x & ~y) & ~(w & ~z)"));
        Assertions.assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(aigCaching))
                .isEqualTo(_c.p.parse("~(a & b & ~c & ~(~x & y))"));
        Assertions.assertThat(_c.p.parse("a | b | (~x & ~y)").transform(aigCaching))
                .isEqualTo(_c.p.parse("~(~a & ~b & ~(~x & ~y))"));
        assertThat(_c.and1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.or1.transform(aigCaching).holds(aigPred)).isTrue();
        Assertions.assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(aigCaching).holds(aigPred))
                .isTrue();
        Assertions.assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(aigCaching).holds(aigPred)).isTrue();
        Assertions.assertThat(_c.p.parse("a | b | (~x & ~y)").transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.and1.holds(aigPred)).isTrue();
        assertThat(_c.f.and(_c.and1, _c.pbc1).holds(aigPred)).isFalse();
        assertThat(_c.or1.holds(aigPred)).isFalse();
        Assertions.assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").holds(aigPred)).isFalse();
        Assertions.assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").holds(aigPred)).isFalse();
        Assertions.assertThat(_c.p.parse("a | b | (~x & ~y)").holds(aigPred)).isFalse();
        final Formula or = _c.p.parse("m | n | o");
        or.transform(aigNonCaching);
        final Formula and = _c.p.parse("m & n & o");
        and.transform(aigNonCaching);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final AIGTransformation aigCaching = new AIGTransformation(_c.f);
        final AIGTransformation aigNonCaching = new AIGTransformation(_c.f, null);

        Assertions.assertThat(_c.p.parse("~a").transform(aigCaching)).isEqualTo(_c.p.parse("~a"));
        Assertions.assertThat(_c.p.parse("~~a").transform(aigCaching)).isEqualTo(_c.p.parse("a"));
        Assertions.assertThat(_c.p.parse("~(a => b)").transform(aigCaching)).isEqualTo(_c.p.parse("a & ~b"));
        Assertions.assertThat(_c.p.parse("~(~(a | b) => ~(x | y))").transform(aigCaching))
                .isEqualTo(_c.p.parse("(~a & ~b) & ~(~x & ~y)"));
        Assertions.assertThat(_c.p.parse("~(a <=> b)").transform(aigCaching))
                .isEqualTo(_c.p.parse("~(~(a & ~b) & ~(~a & b))"));
        Assertions.assertThat(_c.p.parse("~(~(a | b) <=> ~(x | y))").transform(aigCaching))
                .isEqualTo(_c.p.parse("~(~(~a & ~b & ~(~x & ~y)) & ~((a | b) & ~(x | y)))"));
        Assertions.assertThat(_c.p.parse("~(a & b & ~x & ~y)").transform(aigCaching))
                .isEqualTo(_c.p.parse("~(a & b & ~x & ~y)"));
        Assertions.assertThat(_c.p.parse("~(a | b | ~x | ~y)").transform(aigCaching))
                .isEqualTo(_c.p.parse("~a & ~b & x & y"));
        Assertions.assertThat(_c.p.parse("~(a | b | ~x | ~y)").transform(aigCaching))
                .isEqualTo(_c.p.parse("~a & ~b & x & y")); // test caching
        final Formula not = _c.p.parse("~(m | n)");
        not.transform(aigNonCaching);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBC(final FormulaContext _c) {
        final AIGPredicate aigPred = new AIGPredicate(_c.f);
        final AIGTransformation aigCaching = new AIGTransformation(_c.f);
        final AIGTransformation aigNonCaching = new AIGTransformation(_c.f, null);

        assertThat(_c.pbc1.transform(aigCaching).holds(aigPred)).isTrue();
        assertThat(_c.pbc1.transform(aigNonCaching).holds(aigPred)).isTrue();
    }
}
