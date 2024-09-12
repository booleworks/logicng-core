// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class FormulaIteratorTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrue(final FormulaContext _c) {
        final Formula formula = _c.verum;
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFalse(final FormulaContext _c) {
        final Formula formula = _c.falsum;
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiteral(final FormulaContext _c) {
        final Formula formula = _c.a;
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("~(a & (b | c))");
        final Formula operand = _c.p.parse("a & (b | c)");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(operand);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(1);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(operand);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplication(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("a => c | d");
        final Formula left = _c.f.variable("a");
        final Formula right = _c.p.parse("c | d");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(left);
        assertThat(it.next()).isEqualTo(right);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(2);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(left, right);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquivalence(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("a <=> c | d");
        final Formula left = _c.f.variable("a");
        final Formula right = _c.p.parse("c | d");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(left);
        assertThat(it.next()).isEqualTo(right);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(2);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(left, right);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAnd(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("a & (c | d) & ~e");
        final Formula op1 = _c.p.parse("a");
        final Formula op2 = _c.p.parse("c | d");
        final Formula op3 = _c.p.parse("~e");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(op1);
        assertThat(it.next()).isEqualTo(op2);
        assertThat(it.next()).isEqualTo(op3);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(3);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(op1, op2, op3);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testOr(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("a | (c & d) | ~e");
        final Formula op1 = _c.p.parse("a");
        final Formula op2 = _c.p.parse("c & d");
        final Formula op3 = _c.p.parse("~e");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(op1);
        assertThat(it.next()).isEqualTo(op2);
        assertThat(it.next()).isEqualTo(op3);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(3);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(op1, op2, op3);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBC(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("3*a + 4*b + 5*c <= 8");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }
}
