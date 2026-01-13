// SPDX-License-Identifier: _c.apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class Utf8StringRepresentationTest extends TestWithFormulaContext {
    private final FormulaStringRepresentation sr = new Utf8StringRepresentation();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testUTF8Printer(final FormulaContext _c) {
        assertThat(sr.toString(_c.falsum)).isEqualTo("⊥");
        assertThat(sr.toString(_c.verum)).isEqualTo("⊤");
        assertThat(sr.toString(_c.x)).isEqualTo("x");
        assertThat(sr.toString(_c.na)).isEqualTo("¬a");
        assertThat(sr.toString(_c.f.variable("x1"))).isEqualTo("x₁");
        assertThat(sr.toString(_c.f.variable("x190"))).isEqualTo("x₁₉₀");
        assertThat(sr.toString(_c.f.variable("x234"))).isEqualTo("x₂₃₄");
        assertThat(sr.toString(_c.f.variable("x567"))).isEqualTo("x₅₆₇");
        assertThat(sr.toString(_c.f.variable("abc8"))).isEqualTo("abc₈");
        assertThat(sr.toString(_c.imp2)).isEqualTo("¬a ⇒ ¬b");
        assertThat(sr.toString(_c.imp3)).isEqualTo("a ∧ b ⇒ x ∨ y");
        assertThat(sr.toString(_c.eq4)).isEqualTo("a ⇒ b ⇔ ¬a ⇒ ¬b");
        assertThat(sr.toString(_c.and3)).isEqualTo("(x ∨ y) ∧ (¬x ∨ ¬y)");
        assertThat(sr.toString(_c.f.and(_c.a, _c.b, _c.c, _c.x))).isEqualTo("a ∧ b ∧ c ∧ x");
        assertThat(sr.toString(_c.f.or(_c.a, _c.b, _c.c, _c.x))).isEqualTo("a ∨ b ∨ c ∨ x");
        assertThat(sr.toString(_c.pbc1)).isEqualTo("2a + -4b + 3x = 2");
        assertThat(sr.toString(_c.pbc2)).isEqualTo("2a + -4b + 3x > 2");
        assertThat(sr.toString(_c.pbc3)).isEqualTo("2a + -4b + 3x ≥ 2");
        assertThat(sr.toString(_c.pbc4)).isEqualTo("2a + -4b + 3x < 2");
        assertThat(sr.toString(_c.pbc5)).isEqualTo("2a + -4b + 3x ≤ 2");
        assertThat(sr.toString(_c.f.implication(_c.a, _c.f.exo()))).isEqualTo("¬a");
        assertThat(sr.toString(_c.f.equivalence(_c.a, _c.f.exo()))).isEqualTo("¬a");
        assertThat(sr.toString(_c.f.and(_c.a, _c.f.exo()))).isEqualTo("⊥");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.exo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.implication(_c.a, _c.f.amo()))).isEqualTo("⊤");
        assertThat(sr.toString(_c.f.equivalence(_c.a, _c.f.amo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.and(_c.a, _c.f.amo()))).isEqualTo("a");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.amo()))).isEqualTo("⊤");
        assertThat(sr.toString(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b))))
                .isEqualTo("⊤");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSpecialCases(final FormulaContext _c) {
        final Variable var = _c.f.variable("\ntest9t");
        assertThat(sr.toString(var)).isEqualTo("\ntest9t");
    }
}
