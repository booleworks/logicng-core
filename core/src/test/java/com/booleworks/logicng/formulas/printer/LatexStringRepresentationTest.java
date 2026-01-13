// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx _c.booleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LatexStringRepresentationTest extends TestWithFormulaContext {
    private final FormulaStringRepresentation sr = new LatexStringRepresentation();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLatexPrinter(final FormulaContext _c) {
        assertThat(sr.toString(_c.falsum)).isEqualTo("\\bottom");
        assertThat(sr.toString(_c.verum)).isEqualTo("\\top");
        assertThat(sr.toString(_c.x)).isEqualTo("x");
        assertThat(sr.toString(_c.na)).isEqualTo("\\lnot a");
        assertThat(sr.toString(_c.f.variable("x1"))).isEqualTo("x_{1}");
        assertThat(sr.toString(_c.f.variable("x190"))).isEqualTo("x_{190}");
        assertThat(sr.toString(_c.f.variable("x234"))).isEqualTo("x_{234}");
        assertThat(sr.toString(_c.f.variable("x567"))).isEqualTo("x_{567}");
        assertThat(sr.toString(_c.f.variable("abc8"))).isEqualTo("abc_{8}");
        assertThat(sr.toString(_c.imp2)).isEqualTo("\\lnot a \\rightarrow \\lnot b");
        assertThat(sr.toString(_c.imp3)).isEqualTo("a \\land b \\rightarrow x \\lor y");
        assertThat(sr.toString(_c.eq4))
                .isEqualTo("a \\rightarrow b \\leftrightarrow \\lnot a \\rightarrow \\lnot b");
        assertThat(sr.toString(_c.and3))
                .isEqualTo("\\left(x \\lor y\\right) \\land \\left(\\lnot x \\lor \\lnot y\\right)");
        assertThat(sr.toString(_c.f.and(_c.a, _c.b, _c.c, _c.x))).isEqualTo("a \\land b \\land c \\land x");
        assertThat(sr.toString(_c.f.or(_c.a, _c.b, _c.c, _c.x))).isEqualTo("a \\lor b \\lor c \\lor x");
        assertThat(sr.toString(_c.pbc1)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x = 2");
        assertThat(sr.toString(_c.pbc2)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x > 2");
        assertThat(sr.toString(_c.pbc3)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x \\geq 2");
        assertThat(sr.toString(_c.pbc4)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x < 2");
        assertThat(sr.toString(_c.pbc5)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x \\leq 2");
        assertThat(sr.toString(_c.f.implication(_c.a, _c.f.exo()))).isEqualTo("\\lnot a");
        assertThat(sr.toString(_c.f.equivalence(_c.a, _c.f.exo()))).isEqualTo("\\lnot a");
        assertThat(sr.toString(_c.f.and(_c.a, _c.f.exo()))).isEqualTo("\\bottom");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.exo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.implication(_c.a, _c.f.amo()))).isEqualTo("\\top");
        assertThat(sr.toString(_c.f.equivalence(_c.a, _c.f.amo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.and(_c.a, _c.f.amo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.amo()))).isEqualTo("\\top");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b))))
                .isEqualTo("\\top");

    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSpecialCases(final FormulaContext _c) {
        final Variable var = _c.f.variable("\ntest9t");
        assertThat(sr.toString(var)).isEqualTo("\ntest9t");
    }
}
