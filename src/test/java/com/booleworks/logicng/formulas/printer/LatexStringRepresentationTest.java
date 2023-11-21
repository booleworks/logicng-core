// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx _c.booleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LatexStringRepresentationTest extends TestWithFormulaContext {
    private final FormulaStringRepresentation sr = new LatexStringRepresentation();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLatexPrinter(final FormulaContext _c) {
        assertThat(_c.f.string(_c.falsum, sr)).isEqualTo("\\bottom");
        assertThat(_c.f.string(_c.verum, sr)).isEqualTo("\\top");
        assertThat(_c.f.string(_c.x, sr)).isEqualTo("x");
        assertThat(_c.f.string(_c.na, sr)).isEqualTo("\\lnot a");
        assertThat(_c.f.string(_c.f.variable("x1"), sr)).isEqualTo("x_{1}");
        assertThat(_c.f.string(_c.f.variable("x190"), sr)).isEqualTo("x_{190}");
        assertThat(_c.f.string(_c.f.variable("x234"), sr)).isEqualTo("x_{234}");
        assertThat(_c.f.string(_c.f.variable("x567"), sr)).isEqualTo("x_{567}");
        assertThat(_c.f.string(_c.f.variable("abc8"), sr)).isEqualTo("abc_{8}");
        assertThat(_c.f.string(_c.imp2, sr)).isEqualTo("\\lnot a \\rightarrow \\lnot b");
        assertThat(_c.f.string(_c.imp3, sr)).isEqualTo("a \\land b \\rightarrow x \\lor y");
        assertThat(_c.f.string(_c.eq4, sr)).isEqualTo("a \\rightarrow b \\leftrightarrow \\lnot a \\rightarrow \\lnot b");
        assertThat(_c.f.string(_c.and3, sr)).isEqualTo("\\left(x \\lor y\\right) \\land \\left(\\lnot x \\lor \\lnot y\\right)");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("a \\land b \\land c \\land x");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("a \\lor b \\lor c \\lor x");
        assertThat(_c.f.string(_c.pbc1, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x = 2");
        assertThat(_c.f.string(_c.pbc2, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x > 2");
        assertThat(_c.f.string(_c.pbc3, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x \\geq 2");
        assertThat(_c.f.string(_c.pbc4, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x < 2");
        assertThat(_c.f.string(_c.pbc5, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x \\leq 2");
        assertThat(_c.f.string(_c.f.implication(_c.a, _c.f.exo()), sr)).isEqualTo("\\lnot a");
        assertThat(_c.f.string(_c.f.equivalence(_c.a, _c.f.exo()), sr)).isEqualTo("\\lnot a");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.f.exo()), sr)).isEqualTo("\\bottom");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.exo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.implication(_c.a, _c.f.amo()), sr)).isEqualTo("\\top");
        assertThat(_c.f.string(_c.f.equivalence(_c.a, _c.f.amo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.f.amo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo()), sr)).isEqualTo("\\top");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b)), sr)).isEqualTo("\\top");

    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSpecialCases(final FormulaContext _c) {
        final Variable var = _c.f.variable("\ntest9t");
        assertThat(_c.f.string(var, sr)).isEqualTo("\ntest9t");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testViaFormulaFactoryConfig(final FormulaContext _c) {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().stringRepresentation(() -> sr).build());
        assertThat(f.importFormula(_c.eq4).toString()).isEqualTo("a \\rightarrow b \\leftrightarrow \\lnot a \\rightarrow \\lnot b");
    }
}
