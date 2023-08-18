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

public class LatexStringRepresentationTest extends TestWithExampleFormulas {
    private final FormulaStringRepresentation sr = new LatexStringRepresentation();

    @Test
    public void testLatexPrinter() {
        assertThat(f.string(FALSE, sr)).isEqualTo("\\bottom");
        assertThat(f.string(TRUE, sr)).isEqualTo("\\top");
        assertThat(f.string(X, sr)).isEqualTo("x");
        assertThat(f.string(NA, sr)).isEqualTo("\\lnot a");
        assertThat(f.string(f.variable("x1"), sr)).isEqualTo("x_{1}");
        assertThat(f.string(f.variable("x190"), sr)).isEqualTo("x_{190}");
        assertThat(f.string(f.variable("x234"), sr)).isEqualTo("x_{234}");
        assertThat(f.string(f.variable("x567"), sr)).isEqualTo("x_{567}");
        assertThat(f.string(f.variable("abc8"), sr)).isEqualTo("abc_{8}");
        assertThat(f.string(IMP2, sr)).isEqualTo("\\lnot a \\rightarrow \\lnot b");
        assertThat(f.string(IMP3, sr)).isEqualTo("a \\land b \\rightarrow x \\lor y");
        assertThat(f.string(EQ4, sr)).isEqualTo("a \\rightarrow b \\leftrightarrow \\lnot a \\rightarrow \\lnot b");
        assertThat(f.string(AND3, sr)).isEqualTo("\\left(x \\lor y\\right) \\land \\left(\\lnot x \\lor \\lnot y\\right)");
        assertThat(f.string(f.and(A, B, C, X), sr)).isEqualTo("a \\land b \\land c \\land x");
        assertThat(f.string(f.or(A, B, C, X), sr)).isEqualTo("a \\lor b \\lor c \\lor x");
        assertThat(f.string(PBC1, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x = 2");
        assertThat(f.string(PBC2, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x > 2");
        assertThat(f.string(PBC3, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x \\geq 2");
        assertThat(f.string(PBC4, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x < 2");
        assertThat(f.string(PBC5, sr)).isEqualTo("2\\cdot a + -4\\cdot b + 3\\cdot x \\leq 2");
        assertThat(f.string(f.implication(A, f.exo()), sr)).isEqualTo("\\lnot a");
        assertThat(f.string(f.equivalence(A, f.exo()), sr)).isEqualTo("\\lnot a");
        assertThat(f.string(f.and(A, f.exo()), sr)).isEqualTo("\\bottom");
        assertThat(f.string(f.or(A, f.exo()), sr)).isEqualTo("a");
        assertThat(f.string(f.implication(A, f.amo()), sr)).isEqualTo("\\top");
        assertThat(f.string(f.equivalence(A, f.amo()), sr)).isEqualTo("a");
        assertThat(f.string(f.and(A, f.amo()), sr)).isEqualTo("a");
        assertThat(f.string(f.or(A, f.amo()), sr)).isEqualTo("\\top");
        assertThat(f.string(f.or(A, f.amo(), f.exo(), f.equivalence(f.amo(), B)), sr)).isEqualTo("\\top");

    }

    @Test
    public void testSpecialCases() {
        final Variable var = f.variable("\ntest9t");
        assertThat(f.string(var, sr)).isEqualTo("\ntest9t");
        assertThat(sr.toString()).isEqualTo("LatexStringRepresentation");
    }

    @Test
    public void testViaFormulaFactoryConfig() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().stringRepresentation(() -> sr).build());
        assertThat(f.importFormula(EQ4).toString()).isEqualTo("a \\rightarrow b \\leftrightarrow \\lnot a \\rightarrow \\lnot b");
    }
}
