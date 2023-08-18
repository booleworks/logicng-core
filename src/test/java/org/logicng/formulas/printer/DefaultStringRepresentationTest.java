// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.CType;

import java.util.ArrayList;

public class DefaultStringRepresentationTest extends TestWithExampleFormulas {
    private final FormulaStringRepresentation sr = new DefaultStringRepresentation();

    @Test
    public void testDefaultPrinter() {
        assertThat(f.string(FALSE, sr)).isEqualTo("$false");
        assertThat(f.string(TRUE, sr)).isEqualTo("$true");
        assertThat(f.string(X, sr)).isEqualTo("x");
        assertThat(f.string(NA, sr)).isEqualTo("~a");
        assertThat(f.string(f.not(AND1), sr)).isEqualTo("~(a & b)");
        assertThat(f.string(f.variable("x1"), sr)).isEqualTo("x1");
        assertThat(f.string(f.variable("x190"), sr)).isEqualTo("x190");
        assertThat(f.string(f.variable("x234"), sr)).isEqualTo("x234");
        assertThat(f.string(f.variable("x567"), sr)).isEqualTo("x567");
        assertThat(f.string(f.variable("abc8"), sr)).isEqualTo("abc8");
        assertThat(f.string(IMP2, sr)).isEqualTo("~a => ~b");
        assertThat(f.string(IMP3, sr)).isEqualTo("a & b => x | y");
        assertThat(f.string(EQ4, sr)).isEqualTo("a => b <=> ~a => ~b");
        assertThat(f.string(AND3, sr)).isEqualTo("(x | y) & (~x | ~y)");
        assertThat(f.string(f.and(A, B, C, X), sr)).isEqualTo("a & b & c & x");
        assertThat(f.string(f.or(A, B, C, X), sr)).isEqualTo("a | b | c | x");
        assertThat(f.string(PBC1, sr)).isEqualTo("2*a + -4*b + 3*x = 2");
        assertThat(f.string(PBC2, sr)).isEqualTo("2*a + -4*b + 3*x > 2");
        assertThat(f.string(PBC3, sr)).isEqualTo("2*a + -4*b + 3*x >= 2");
        assertThat(f.string(PBC4, sr)).isEqualTo("2*a + -4*b + 3*x < 2");
        assertThat(f.string(PBC5, sr)).isEqualTo("2*a + -4*b + 3*x <= 2");
        assertThat(f.string(f.pbc(CType.LT, 42, new ArrayList<>(), new ArrayList<>()), sr)).isEqualTo("$true");
        assertThat(f.string(f.pbc(CType.EQ, 42, new ArrayList<>(), new ArrayList<>()), sr)).isEqualTo("$false");
        assertThat(f.string(f.implication(A, f.exo()), sr)).isEqualTo("~a");
        assertThat(f.string(f.equivalence(A, f.exo()), sr)).isEqualTo("~a");
        assertThat(f.string(f.and(A, f.exo()), sr)).isEqualTo("$false");
        assertThat(f.string(f.or(A, f.exo()), sr)).isEqualTo("a");
        assertThat(f.string(f.implication(A, f.amo()), sr)).isEqualTo("$true");
        assertThat(f.string(f.equivalence(A, f.amo()), sr)).isEqualTo("a");
        assertThat(f.string(f.and(A, f.amo()), sr)).isEqualTo("a");
        assertThat(f.string(f.or(A, f.amo()), sr)).isEqualTo("$true");
        assertThat(f.string(f.or(A, f.amo(), f.exo(), f.equivalence(f.amo(), B)), sr)).isEqualTo("$true");
    }

    @Test
    public void testToString() {
        assertThat(sr.toString()).isEqualTo("DefaultStringRepresentation");
    }

    @Test
    public void testFFDefaultStringRepresentation() {
        assertThat(EQ4.toString()).isEqualTo("a => b <=> ~a => ~b");
    }
}
