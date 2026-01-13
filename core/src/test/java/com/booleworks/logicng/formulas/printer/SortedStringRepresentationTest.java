// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SortedStringRepresentationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulaComparator(final FormulaContext _c) {
        final List<Variable> varOrder = new ArrayList<>(Arrays.asList(_c.y, _c.x, _c.b, _c.a, _c.c));
        final SortedStringRepresentation.FormulaComparator comparator =
                new SortedStringRepresentation.FormulaComparator(_c.f, varOrder);
        assertThat(comparator.compare(_c.falsum, _c.verum)).isZero();
        assertThat(comparator.compare(_c.a, _c.a)).isZero();
        assertThat(comparator.compare(_c.a, _c.b)).isPositive();
        assertThat(comparator.compare(_c.a, _c.c)).isNegative();
        assertThat(comparator.compare(_c.a, _c.and1)).isPositive();
        assertThat(comparator.compare(_c.b, _c.and1)).isNegative();
        assertThat(comparator.compare(_c.eq4, _c.imp4)).isPositive();
        assertThat(comparator.compare(_c.and3, _c.imp4)).isNegative();
        assertThat(comparator.compare(_c.and3, _c.imp4)).isNegative();
        assertThat(comparator.compare(_c.not1, _c.not2)).isPositive();
        assertThat(comparator.compare(_c.or1, _c.pbc2)).isNegative();
        assertThat(comparator.compare(_c.pbc1, _c.pbc2)).isZero();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSortedPrinter(final FormulaContext _c) {
        final List<Variable> varOrder = new ArrayList<>(Arrays.asList(_c.y, _c.x, _c.b, _c.a, _c.c));
        final FormulaStringRepresentation sr = new SortedStringRepresentation(_c.f, varOrder);
        assertThat(sr.toString(_c.falsum)).isEqualTo("$false");
        assertThat(sr.toString(_c.verum)).isEqualTo("$true");
        assertThat(sr.toString(_c.x)).isEqualTo("x");
        assertThat(sr.toString(_c.na)).isEqualTo("~a");
        assertThat(sr.toString(_c.imp2)).isEqualTo("~a => ~b");
        assertThat(sr.toString(_c.f.not(_c.f.equivalence(_c.a, _c.b)))).isEqualTo("~(b <=> a)");
        assertThat(sr.toString(_c.imp3)).isEqualTo("b & a => y | x");
        assertThat(sr.toString(_c.eq3)).isEqualTo("y | x <=> b & a");
        assertThat(sr.toString(_c.eq4)).isEqualTo("a => b <=> ~a => ~b");
        assertThat(sr.toString(_c.and3)).isEqualTo("(y | x) & (~y | ~x)");
        assertThat(sr.toString(_c.f.and(_c.a, _c.b, _c.c, _c.x))).isEqualTo("x & b & a & c");
        assertThat(sr.toString(_c.f.or(_c.a, _c.b, _c.c, _c.x))).isEqualTo("x | b | a | c");
        assertThat(sr.toString(_c.pbc2)).isEqualTo("3*x + -4*b + 2*a > 2");
        assertThat(sr.toString(_c.f.and(_c.nb, _c.pbc1))).isEqualTo("(3*x + -4*b + 2*a = 2) & ~b");
        assertThat(sr.toString(_c.f.pbc(CType.EQ, 42, new ArrayList<>(Arrays.asList(_c.a, _c.b)),
                new ArrayList<>(Arrays.asList(1, 1))))).isEqualTo("b + a = 42");
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

        // some variables not in varOrder
        varOrder.remove(_c.x);
        assertThat(sr.toString(_c.f.or(_c.f.variable("x"), _c.f.variable("a")))).isEqualTo("a | x");
        assertThat(sr.toString(_c.pbc2)).isEqualTo("-4*b + 2*a + 3*x > 2");

        // empty varOrder
        assertThat(new SortedStringRepresentation(_c.f, new ArrayList<>()).toString(_c.eq3)).isEqualTo(
                "a & b <=> x | y");
    }
}
