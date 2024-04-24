// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashSet;

public class SubformulaTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        assertThat(_c.verum.containsNode(_c.verum)).isTrue();
        assertThat(_c.falsum.containsNode(_c.falsum)).isTrue();
        assertThat(_c.verum.containsNode(_c.falsum)).isFalse();
        assertThat(_c.falsum.containsNode(_c.verum)).isFalse();
        assertThat(_c.falsum.containsNode(_c.a)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.a.containsNode(_c.a)).isTrue();
        assertThat(_c.a.containsNode(_c.f.variable("a"))).isTrue();
        assertThat(_c.na.containsNode(_c.a)).isFalse();
        assertThat(_c.na.containsNode(_c.f.literal("a", false))).isTrue();
        assertThat(_c.a.containsNode(_c.na)).isFalse();
        assertThat(_c.a.containsNode(_c.b)).isFalse();
        assertThat(_c.na.containsNode(_c.nb)).isFalse();
        assertThat(_c.a.containsNode(_c.falsum)).isFalse();
        assertThat(_c.na.containsNode(_c.verum)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) {
        assertThat(_c.not1.containsNode(_c.not1)).isTrue();
        assertThat(_c.not1.containsNode(_c.f.not(_c.and1))).isTrue();
        assertThat(_c.not1.containsNode(_c.and1)).isTrue();
        assertThat(_c.not1.containsNode(_c.a)).isTrue();
        assertThat(_c.not1.containsNode(_c.f.variable("b"))).isTrue();
        assertThat(_c.not2.containsNode(_c.not2)).isTrue();
        assertThat(_c.not2.containsNode(_c.or1)).isTrue();
        assertThat(_c.not2.containsNode(_c.x)).isTrue();
        assertThat(_c.not2.containsNode(_c.y)).isTrue();

        assertThat(_c.not1.containsNode(_c.or1)).isFalse();
        assertThat(_c.not1.containsNode(_c.x)).isFalse();
        assertThat(_c.not2.containsNode(_c.not1)).isFalse();
        assertThat(_c.not2.containsNode(_c.and1)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplication(final FormulaContext _c) {
        assertThat(_c.imp1.containsNode(_c.imp1)).isTrue();
        assertThat(_c.imp1.containsNode(_c.f.implication(_c.a, _c.b))).isTrue();
        assertThat(_c.imp2.containsNode(_c.imp2)).isTrue();
        assertThat(_c.imp3.containsNode(_c.imp3)).isTrue();
        assertThat(_c.imp4.containsNode(_c.imp4)).isTrue();
        assertThat(_c.imp1.containsNode(_c.a)).isTrue();
        assertThat(_c.imp1.containsNode(_c.b)).isTrue();
        assertThat(_c.imp2.containsNode(_c.na)).isTrue();
        assertThat(_c.imp2.containsNode(_c.nb)).isTrue();
        assertThat(_c.imp2.containsNode(_c.a)).isFalse();
        assertThat(_c.imp2.containsNode(_c.b)).isFalse();
        assertThat(_c.imp3.containsNode(_c.and1)).isTrue();
        assertThat(_c.imp3.containsNode(_c.or1)).isTrue();
        assertThat(_c.imp3.containsNode(_c.a)).isTrue();
        assertThat(_c.imp3.containsNode(_c.b)).isTrue();
        assertThat(_c.imp3.containsNode(_c.x)).isTrue();
        assertThat(_c.imp3.containsNode(_c.y)).isTrue();
        assertThat(_c.imp4.containsNode(_c.f.equivalence(_c.a, _c.b))).isTrue();
        assertThat(_c.imp4.containsNode(_c.f.equivalence(_c.nx, _c.ny))).isTrue();

        assertThat(_c.imp4.containsNode(_c.c)).isFalse();
        assertThat(_c.imp4.containsNode(_c.not1)).isFalse();
        assertThat(_c.imp4.containsNode(_c.f.equivalence(_c.x, _c.ny))).isFalse();
        assertThat(_c.imp4.containsNode(_c.f.equivalence(_c.ny, _c.x))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquivalence(final FormulaContext _c) {
        assertThat(_c.eq1.containsNode(_c.eq1)).isTrue();
        assertThat(_c.eq1.containsNode(_c.f.equivalence(_c.a, _c.b))).isTrue();
        assertThat(_c.eq4.containsNode(_c.imp1)).isTrue();
        assertThat(_c.eq4.containsNode(_c.imp2)).isTrue();
        assertThat(_c.eq4.containsNode(_c.a)).isTrue();
        assertThat(_c.eq4.containsNode(_c.b)).isTrue();

        assertThat(_c.eq2.containsNode(_c.c)).isFalse();
        assertThat(_c.eq2.containsNode(_c.not1)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testOr(final FormulaContext _c) {
        assertThat(_c.or1.containsNode(_c.f.or(_c.x, _c.y))).isTrue();
        assertThat(_c.or1.containsNode(_c.x)).isTrue();
        assertThat(_c.or1.containsNode(_c.f.variable("y"))).isTrue();
        assertThat(_c.or3.containsNode(_c.and1)).isTrue();
        assertThat(_c.or3.containsNode(_c.and2)).isTrue();
        assertThat(_c.or3.containsNode(_c.na)).isTrue();
        assertThat(_c.or3.containsNode(_c.nb)).isTrue();
        assertThat(_c.or3.containsNode(_c.a)).isTrue();
        assertThat(_c.or3.containsNode(_c.b)).isTrue();
        assertThat(_c.f.or(_c.a, _c.b, _c.nx, _c.ny, _c.c).containsNode(_c.f.or(_c.a, _c.nx, _c.c))).isTrue();
        assertThat(_c.f.or(_c.a, _c.b, _c.or1, _c.c, _c.and1).containsNode(_c.f.or(_c.x, _c.y, _c.and1))).isTrue();
        assertThat(_c.f.or(_c.a, _c.b, _c.or1, _c.c, _c.and1).containsNode(_c.f.or(_c.a, _c.and1, _c.x))).isTrue();

        assertThat(_c.f.or(_c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.f.or(_c.a, _c.b))).isFalse();
        assertThat(_c.f.or(_c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.ny)).isFalse();
        assertThat(_c.f.or(_c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.f.or(_c.a, _c.c))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAnd(final FormulaContext _c) {
        assertThat(_c.and1.containsNode(_c.f.and(_c.a, _c.b))).isTrue();
        assertThat(_c.and1.containsNode(_c.a)).isTrue();
        assertThat(_c.and1.containsNode(_c.f.variable("b"))).isTrue();
        assertThat(_c.and3.containsNode(_c.or1)).isTrue();
        assertThat(_c.and3.containsNode(_c.or2)).isTrue();
        assertThat(_c.and3.containsNode(_c.nx)).isTrue();
        assertThat(_c.and3.containsNode(_c.ny)).isTrue();
        assertThat(_c.and3.containsNode(_c.x)).isTrue();
        assertThat(_c.and3.containsNode(_c.y)).isTrue();
        assertThat(_c.f.and(_c.a, _c.b, _c.nx, _c.ny, _c.c).containsNode(_c.f.and(_c.a, _c.nx, _c.c))).isTrue();
        assertThat(_c.f.and(_c.x, _c.y, _c.or1, _c.c, _c.and1).containsNode(_c.f.and(_c.a, _c.b, _c.c))).isTrue();
        assertThat(_c.f.and(_c.a, _c.b, _c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.f.and(_c.a, _c.or1, _c.nx)))
                .isTrue();
        assertThat(_c.f.and(_c.a, _c.b, _c.nx, _c.imp1, _c.c).containsNode(_c.imp1)).isTrue();

        assertThat(_c.f.and(_c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.f.or(_c.a, _c.b))).isFalse();
        assertThat(_c.f.and(_c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.ny)).isFalse();
        assertThat(_c.f.and(_c.nx, _c.or1, _c.c, _c.and1).containsNode(_c.f.or(_c.a, _c.c))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void subformulasTest(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.p.parse("((a & ~b & c) | (d & (~e | c))) & (a => (~x | y) & (x | ~z))");
        final LinkedHashSet<Formula> expected = new LinkedHashSet<>();
        expected.add(_c.p.parse("a"));
        expected.add(_c.p.parse("~b"));
        expected.add(_c.p.parse("c"));
        expected.add(_c.p.parse("a & ~b & c"));
        expected.add(_c.p.parse("d"));
        expected.add(_c.p.parse("~e"));
        expected.add(_c.p.parse("~e | c"));
        expected.add(_c.p.parse("d & (~e | c)"));
        expected.add(_c.p.parse("(a & ~b & c) | (d & (~e | c))"));
        expected.add(_c.p.parse("~x"));
        expected.add(_c.p.parse("y"));
        expected.add(_c.p.parse("~x | y"));
        expected.add(_c.p.parse("x"));
        expected.add(_c.p.parse("~z"));
        expected.add(_c.p.parse("x | ~z"));
        expected.add(_c.p.parse("(~x | y) & (x | ~z)"));
        expected.add(_c.p.parse("a => (~x | y) & (x | ~z)"));
        expected.add(_c.p.parse("((a & ~b & c) | (d & (~e | c))) & (a => (~x | y) & (x | ~z))"));
        Assertions.assertThat(f1.apply(new SubNodeFunction(_c.f))).isEqualTo(expected);
    }

    @Test
    public void testNoCache() throws ParserException {
        final CachingFormulaFactory f = FormulaFactory.caching();
        final Formula f1 = f.parse("(d | (a & b)) & (c | (a & b)) | (a & b )");
        f1.apply(new SubNodeFunction(f, null));
        assertThat(f.getFunctionCacheForType(FunctionCacheEntry.SUBFORMULAS).get(f1)).isNull();
    }
}
