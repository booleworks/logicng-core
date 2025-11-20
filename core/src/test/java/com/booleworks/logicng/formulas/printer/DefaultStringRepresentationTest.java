// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;

public class DefaultStringRepresentationTest extends TestWithFormulaContext {
    private final FormulaStringRepresentation sr = new DefaultStringRepresentation();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testDefaultPrinter(final FormulaContext _c) {
        assertThat(sr.toString(_c.falsum)).isEqualTo("$false");
        assertThat(sr.toString(_c.verum)).isEqualTo("$true");
        assertThat(sr.toString(_c.x)).isEqualTo("x");
        assertThat(sr.toString(_c.na)).isEqualTo("~a");
        assertThat(sr.toString(_c.f.not(_c.and1))).isEqualTo("~(a & b)");
        assertThat(sr.toString(_c.f.variable("x1"))).isEqualTo("x1");
        assertThat(sr.toString(_c.f.variable("x190"))).isEqualTo("x190");
        assertThat(sr.toString(_c.f.variable("x234"))).isEqualTo("x234");
        assertThat(sr.toString(_c.f.variable("x567"))).isEqualTo("x567");
        assertThat(sr.toString(_c.f.variable("abc8"))).isEqualTo("abc8");
        assertThat(sr.toString(_c.imp2)).isEqualTo("~a => ~b");
        assertThat(sr.toString(_c.imp3)).isEqualTo("a & b => x | y");
        assertThat(sr.toString(_c.eq4)).isEqualTo("a => b <=> ~a => ~b");
        assertThat(sr.toString(_c.and3)).isEqualTo("(x | y) & (~x | ~y)");
        assertThat(sr.toString(_c.f.and(_c.a, _c.b, _c.c, _c.x))).isEqualTo("a & b & c & x");
        assertThat(sr.toString(_c.f.or(_c.a, _c.b, _c.c, _c.x))).isEqualTo("a | b | c | x");
        assertThat(sr.toString(_c.pbc1)).isEqualTo("2*a + -4*b + 3*x = 2");
        assertThat(sr.toString(_c.pbc2)).isEqualTo("2*a + -4*b + 3*x > 2");
        assertThat(sr.toString(_c.pbc3)).isEqualTo("2*a + -4*b + 3*x >= 2");
        assertThat(sr.toString(_c.pbc4)).isEqualTo("2*a + -4*b + 3*x < 2");
        assertThat(sr.toString(_c.pbc5)).isEqualTo("2*a + -4*b + 3*x <= 2");
        assertThat(sr.toString(_c.f.pbc(CType.LT, 42, new ArrayList<>(), new ArrayList<>()))).isEqualTo("$true");
        assertThat(sr.toString(_c.f.pbc(CType.EQ, 42, new ArrayList<>(), new ArrayList<>()))).isEqualTo("$false");
        assertThat(sr.toString(_c.f.implication(_c.a, _c.f.exo()))).isEqualTo("~a");
        assertThat(sr.toString(_c.f.equivalence(_c.a, _c.f.exo()))).isEqualTo("~a");
        assertThat(sr.toString(_c.f.and(_c.a, _c.f.exo()))).isEqualTo("$false");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.exo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.implication(_c.a, _c.f.amo()))).isEqualTo("$true");
        assertThat(sr.toString(_c.f.equivalence(_c.a, _c.f.amo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.and(_c.a, _c.f.amo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.amo()))).isEqualTo("$true");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b))))
                .isEqualTo("$true");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFFDefaultStringRepresentation(final FormulaContext _c) {
        assertThat(_c.eq4.toString()).isEqualTo("a => b <=> ~a => ~b");
    }
}
