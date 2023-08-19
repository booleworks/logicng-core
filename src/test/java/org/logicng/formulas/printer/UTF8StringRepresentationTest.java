// SPDX-License-Identifier: _c.apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaFactoryConfig;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;

public class UTF8StringRepresentationTest extends TestWithFormulaContext {
    private final FormulaStringRepresentation sr = new UTF8StringRepresentation();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testUTF8Printer(final FormulaContext _c) {
        assertThat(_c.f.string(_c.falsum, sr)).isEqualTo("⊥");
        assertThat(_c.f.string(_c.verum, sr)).isEqualTo("⊤");
        assertThat(_c.f.string(_c.x, sr)).isEqualTo("x");
        assertThat(_c.f.string(_c.na, sr)).isEqualTo("¬a");
        assertThat(_c.f.string(_c.f.variable("x1"), sr)).isEqualTo("x₁");
        assertThat(_c.f.string(_c.f.variable("x190"), sr)).isEqualTo("x₁₉₀");
        assertThat(_c.f.string(_c.f.variable("x234"), sr)).isEqualTo("x₂₃₄");
        assertThat(_c.f.string(_c.f.variable("x567"), sr)).isEqualTo("x₅₆₇");
        assertThat(_c.f.string(_c.f.variable("abc8"), sr)).isEqualTo("abc₈");
        assertThat(_c.f.string(_c.imp2, sr)).isEqualTo("¬a ⇒ ¬b");
        assertThat(_c.f.string(_c.imp3, sr)).isEqualTo("a ∧ b ⇒ x ∨ y");
        assertThat(_c.f.string(_c.eq4, sr)).isEqualTo("a ⇒ b ⇔ ¬a ⇒ ¬b");
        assertThat(_c.f.string(_c.and3, sr)).isEqualTo("(x ∨ y) ∧ (¬x ∨ ¬y)");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("a ∧ b ∧ c ∧ x");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("a ∨ b ∨ c ∨ x");
        assertThat(_c.f.string(_c.pbc1, sr)).isEqualTo("2a + -4b + 3x = 2");
        assertThat(_c.f.string(_c.pbc2, sr)).isEqualTo("2a + -4b + 3x > 2");
        assertThat(_c.f.string(_c.pbc3, sr)).isEqualTo("2a + -4b + 3x ≥ 2");
        assertThat(_c.f.string(_c.pbc4, sr)).isEqualTo("2a + -4b + 3x < 2");
        assertThat(_c.f.string(_c.pbc5, sr)).isEqualTo("2a + -4b + 3x ≤ 2");
        assertThat(_c.f.string(_c.f.implication(_c.a, _c.f.exo()), sr)).isEqualTo("¬a");
        assertThat(_c.f.string(_c.f.equivalence(_c.a, _c.f.exo()), sr)).isEqualTo("¬a");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.f.exo()), sr)).isEqualTo("⊥");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.exo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.implication(_c.a, _c.f.amo()), sr)).isEqualTo("⊤");
        assertThat(_c.f.string(_c.f.equivalence(_c.a, _c.f.amo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.f.amo()), sr)).isEqualTo("a");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo()), sr)).isEqualTo("⊤");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b)), sr)).isEqualTo("⊤");
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
        assertThat(f.importFormula(_c.eq4).toString()).isEqualTo("a ⇒ b ⇔ ¬a ⇒ ¬b");
    }
}
