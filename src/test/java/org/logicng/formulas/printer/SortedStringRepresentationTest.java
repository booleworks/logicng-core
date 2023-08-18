// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.CType;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaFactoryConfig;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SortedStringRepresentationTest extends TestWithExampleFormulas {

    private final List<Variable> varOrder = new ArrayList<>(Arrays.asList(Y, X, B, A, C));

    private final FormulaStringRepresentation sr = new SortedStringRepresentation(varOrder);

    @Test
    public void testFormulaComparator() {
        final SortedStringRepresentation.FormulaComparator comparator = new SortedStringRepresentation.FormulaComparator(varOrder);
        assertThat(comparator.compare(FALSE, TRUE)).isZero();
        assertThat(comparator.compare(A, A)).isZero();
        assertThat(comparator.compare(A, B)).isPositive();
        assertThat(comparator.compare(A, C)).isNegative();
        assertThat(comparator.compare(A, AND1)).isPositive();
        assertThat(comparator.compare(B, AND1)).isNegative();
        assertThat(comparator.compare(EQ4, IMP4)).isPositive();
        assertThat(comparator.compare(AND3, IMP4)).isNegative();
        assertThat(comparator.compare(AND3, IMP4)).isNegative();
        assertThat(comparator.compare(NOT1, NOT2)).isPositive();
        assertThat(comparator.compare(OR1, PBC2)).isNegative();
        assertThat(comparator.compare(PBC1, PBC2)).isZero();
    }

    @Test
    public void testSortedPrinter() {
        assertThat(f.string(FALSE, sr)).isEqualTo("$false");
        assertThat(f.string(TRUE, sr)).isEqualTo("$true");
        assertThat(f.string(X, sr)).isEqualTo("x");
        assertThat(f.string(NA, sr)).isEqualTo("~a");
        assertThat(f.string(IMP2, sr)).isEqualTo("~a => ~b");
        assertThat(f.string(f.not(f.equivalence(A, B)), sr)).isEqualTo("~(b <=> a)");
        assertThat(f.string(IMP3, sr)).isEqualTo("b & a => y | x");
        assertThat(f.string(EQ3, sr)).isEqualTo("y | x <=> b & a");
        assertThat(f.string(EQ4, sr)).isEqualTo("a => b <=> ~a => ~b");
        assertThat(f.string(AND3, sr)).isEqualTo("(y | x) & (~y | ~x)");
        assertThat(f.string(f.and(A, B, C, X), sr)).isEqualTo("x & b & a & c");
        assertThat(f.string(f.or(A, B, C, X), sr)).isEqualTo("x | b | a | c");
        assertThat(f.string(PBC2, sr)).isEqualTo("3*x + -4*b + 2*a > 2");
        assertThat(f.string(f.and(NB, PBC1), sr)).isEqualTo("(3*x + -4*b + 2*a = 2) & ~b");
        assertThat(f.string(f.pbc(CType.EQ, 42, new ArrayList<Literal>(Arrays.asList(A, B)), new ArrayList<>(Arrays.asList(1, 1))), sr)).isEqualTo("b + a = 42");
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

        // some variables not in varOrder
        varOrder.remove(X);
        assertThat(f.string(f.or(f.variable("x"), f.variable("a")), sr)).isEqualTo("a | x");
        assertThat(f.string(PBC2, sr)).isEqualTo("-4*b + 2*a + 3*x > 2");

        // empty varOrder
        assertThat(f.string(EQ3, new SortedStringRepresentation(new ArrayList<>()))).isEqualTo("a & b <=> x | y");
    }

    @Test
    public void testToString() {
        assertThat(sr.toString()).isEqualTo("SortedStringRepresentation");
    }

    @Test
    public void testViaFormulaFactoryConfig() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().stringRepresentation(() -> sr).build());
        assertThat(f.importFormula(EQ4).toString()).isEqualTo("a => b <=> ~a => ~b");
    }
}
