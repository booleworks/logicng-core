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
        assertThat(_c.f.string(_c.falsum, sr)).isEqualTo("$false");
        assertThat(_c.f.string(_c.verum, sr)).isEqualTo("$true");
        assertThat(_c.f.string(_c.x, sr)).isEqualTo("x");
        assertThat(_c.f.string(_c.na, sr)).isEqualTo("~a");
        assertThat(_c.f.string(_c.f.not(_c.and1), sr)).isEqualTo("~(a & b)");
        assertThat(_c.f.string(_c.f.variable("x1"), sr)).isEqualTo("x1");
        assertThat(_c.f.string(_c.f.variable("x190"), sr)).isEqualTo("x190");
        assertThat(_c.f.string(_c.f.variable("x234"), sr)).isEqualTo("x234");
        assertThat(_c.f.string(_c.f.variable("x567"), sr)).isEqualTo("x567");
        assertThat(_c.f.string(_c.f.variable("abc8"), sr)).isEqualTo("abc8");
        assertThat(_c.f.string(_c.imp2, sr)).isEqualTo("~a => ~b");
        assertThat(_c.f.string(_c.imp3, sr)).isEqualTo("a & b => x | y");
        assertThat(_c.f.string(_c.eq4, sr)).isEqualTo("a => b <=> ~a => ~b");
        assertThat(_c.f.string(_c.and3, sr)).isEqualTo("(x | y) & (~x | ~y)");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("a & b & c & x");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("a | b | c | x");
        assertThat(_c.f.string(_c.pbc1, sr)).isEqualTo("2*a + -4*b + 3*x = 2");
        assertThat(_c.f.string(_c.pbc2, sr)).isEqualTo("2*a + -4*b + 3*x > 2");
        assertThat(_c.f.string(_c.pbc3, sr)).isEqualTo("2*a + -4*b + 3*x >= 2");
        assertThat(_c.f.string(_c.pbc4, sr)).isEqualTo("2*a + -4*b + 3*x < 2");
        assertThat(_c.f.string(_c.pbc5, sr)).isEqualTo("2*a + -4*b + 3*x <= 2");
        assertThat(_c.f.string(_c.f.pbc(CType.LT, 42, new ArrayList<>(), new ArrayList<>()), sr)).isEqualTo("$true");
        assertThat(_c.f.string(_c.f.pbc(CType.EQ, 42, new ArrayList<>(), new ArrayList<>()), sr)).isEqualTo("$false");
        assertThat(_c.f.string(_c.f.implication(_c.a, _c.f.exo()), sr)).isEqualTo("~a");
        assertThat(_c.f.string(_c.f.equivalence(_c.a, _c.f.exo()), sr)).isEqualTo("~a");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.f.exo()), sr)).isEqualTo("$false");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.exo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.implication(_c.a, _c.f.amo()), sr)).isEqualTo("$true");
        assertThat(_c.f.string(_c.f.equivalence(_c.a, _c.f.amo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.f.amo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo()), sr)).isEqualTo("$true");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b)), sr))
                .isEqualTo("$true");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFFDefaultStringRepresentation(final FormulaContext _c) {
        assertThat(_c.eq4.toString()).isEqualTo("a => b <=> ~a => ~b");
    }
}
