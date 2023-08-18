// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaFactoryConfig;
import org.logicng.formulas.Variable;

public class UTF8StringRepresentationTest extends TestWithExampleFormulas {
    private final FormulaStringRepresentation sr = new UTF8StringRepresentation();

    @Test
    public void testUTF8Printer() {
        assertThat(f.string(FALSE, sr)).isEqualTo("⊥");
        assertThat(f.string(TRUE, sr)).isEqualTo("⊤");
        assertThat(f.string(X, sr)).isEqualTo("x");
        assertThat(f.string(NA, sr)).isEqualTo("¬a");
        assertThat(f.string(f.variable("x1"), sr)).isEqualTo("x₁");
        assertThat(f.string(f.variable("x190"), sr)).isEqualTo("x₁₉₀");
        assertThat(f.string(f.variable("x234"), sr)).isEqualTo("x₂₃₄");
        assertThat(f.string(f.variable("x567"), sr)).isEqualTo("x₅₆₇");
        assertThat(f.string(f.variable("abc8"), sr)).isEqualTo("abc₈");
        assertThat(f.string(IMP2, sr)).isEqualTo("¬a ⇒ ¬b");
        assertThat(f.string(IMP3, sr)).isEqualTo("a ∧ b ⇒ x ∨ y");
        assertThat(f.string(EQ4, sr)).isEqualTo("a ⇒ b ⇔ ¬a ⇒ ¬b");
        assertThat(f.string(AND3, sr)).isEqualTo("(x ∨ y) ∧ (¬x ∨ ¬y)");
        assertThat(f.string(f.and(A, B, C, X), sr)).isEqualTo("a ∧ b ∧ c ∧ x");
        assertThat(f.string(f.or(A, B, C, X), sr)).isEqualTo("a ∨ b ∨ c ∨ x");
        assertThat(f.string(PBC1, sr)).isEqualTo("2a + -4b + 3x = 2");
        assertThat(f.string(PBC2, sr)).isEqualTo("2a + -4b + 3x > 2");
        assertThat(f.string(PBC3, sr)).isEqualTo("2a + -4b + 3x ≥ 2");
        assertThat(f.string(PBC4, sr)).isEqualTo("2a + -4b + 3x < 2");
        assertThat(f.string(PBC5, sr)).isEqualTo("2a + -4b + 3x ≤ 2");
        assertThat(f.string(f.implication(A, f.exo()), sr)).isEqualTo("¬a");
        assertThat(f.string(f.equivalence(A, f.exo()), sr)).isEqualTo("¬a");
        assertThat(f.string(f.and(A, f.exo()), sr)).isEqualTo("⊥");
        assertThat(f.string(f.or(A, f.exo()), sr)).isEqualTo("a");
        assertThat(f.string(f.implication(A, f.amo()), sr)).isEqualTo("⊤");
        assertThat(f.string(f.equivalence(A, f.amo()), sr)).isEqualTo("a");
        assertThat(f.string(f.and(A, f.amo()), sr)).isEqualTo("a");
        assertThat(f.string(f.or(A, f.amo()), sr)).isEqualTo("⊤");
        assertThat(f.string(f.or(A, f.amo(), f.exo(), f.equivalence(f.amo(), B)), sr)).isEqualTo("⊤");
    }

    @Test
    public void testSpecialCases() {
        final Variable var = f.variable("\ntest9t");
        assertThat(f.string(var, sr)).isEqualTo("\ntest9t");
        assertThat(sr.toString()).isEqualTo("UTF8StringRepresentation");
    }

    @Test
    public void testViaFormulaFactoryConfig() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().stringRepresentation(() -> sr).build());
        assertThat(f.importFormula(EQ4).toString()).isEqualTo("a ⇒ b ⇔ ¬a ⇒ ¬b");
    }
}
