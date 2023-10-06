// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.formulas.CType;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaFactoryConfig;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SortedStringRepresentationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulaComparator(final FormulaContext _c) {
        final List<Variable> varOrder = new ArrayList<>(Arrays.asList(_c.y, _c.x, _c.b, _c.a, _c.c));
        final SortedStringRepresentation.FormulaComparator comparator = new SortedStringRepresentation.FormulaComparator(_c.f, varOrder);
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
        assertThat(_c.f.string(_c.falsum, sr)).isEqualTo("$false");
        assertThat(_c.f.string(_c.verum, sr)).isEqualTo("$true");
        assertThat(_c.f.string(_c.x, sr)).isEqualTo("x");
        assertThat(_c.f.string(_c.na, sr)).isEqualTo("~a");
        assertThat(_c.f.string(_c.imp2, sr)).isEqualTo("~a => ~b");
        assertThat(_c.f.string(_c.f.not(_c.f.equivalence(_c.a, _c.b)), sr)).isEqualTo("~(b <=> a)");
        assertThat(_c.f.string(_c.imp3, sr)).isEqualTo("b & a => y | x");
        assertThat(_c.f.string(_c.eq3, sr)).isEqualTo("y | x <=> b & a");
        assertThat(_c.f.string(_c.eq4, sr)).isEqualTo("a => b <=> ~a => ~b");
        assertThat(_c.f.string(_c.and3, sr)).isEqualTo("(y | x) & (~y | ~x)");
        assertThat(_c.f.string(_c.f.and(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("x & b & a & c");
        assertThat(_c.f.string(_c.f.or(_c.a, _c.b, _c.c, _c.x), sr)).isEqualTo("x | b | a | c");
        assertThat(_c.f.string(_c.pbc2, sr)).isEqualTo("3*x + -4*b + 2*a > 2");
        assertThat(_c.f.string(_c.f.and(_c.nb, _c.pbc1), sr)).isEqualTo("(3*x + -4*b + 2*a = 2) & ~b");
        assertThat(_c.f.string(_c.f.pbc(CType.EQ, 42, new ArrayList<>(Arrays.asList(_c.a, _c.b)), new ArrayList<>(Arrays.asList(1, 1))), sr)).isEqualTo("b + a = 42");
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
        assertThat(_c.f.string(_c.f.or(_c.a, _c.f.amo(), _c.f.exo(), _c.f.equivalence(_c.f.amo(), _c.b)), sr)).isEqualTo("$true");

        // some variables not in varOrder
        varOrder.remove(_c.x);
        assertThat(_c.f.string(_c.f.or(_c.f.variable("x"), _c.f.variable("a")), sr)).isEqualTo("a | x");
        assertThat(_c.f.string(_c.pbc2, sr)).isEqualTo("-4*b + 2*a + 3*x > 2");

        // empty varOrder
        assertThat(_c.f.string(_c.eq3, new SortedStringRepresentation(_c.f, new ArrayList<>()))).isEqualTo("a & b <=> x | y");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testViaFormulaFactoryConfig(final FormulaContext _c) {
        final List<Variable> varOrder = new ArrayList<>(Arrays.asList(_c.y, _c.x, _c.b, _c.a, _c.c));
        final FormulaStringRepresentation sr = new SortedStringRepresentation(_c.f, varOrder);
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().stringRepresentation(() -> sr).build());
        assertThat(f.importFormula(_c.eq4).toString()).isEqualTo("a => b <=> ~a => ~b");
    }
}
