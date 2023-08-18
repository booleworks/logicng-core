// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.parsers.PseudoBooleanParser;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class FormulaIteratorTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final PropositionalParser p = new PropositionalParser(f);

    @Test
    public void testTrue() {
        final Formula formula = f.verum();
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testFalse() {
        final Formula formula = f.falsum();
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testLiteral() {
        final Formula formula = f.variable("a");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testNot() throws ParserException {
        final Formula formula = p.parse("~(a & (b | c))");
        final Formula operand = p.parse("a & (b | c)");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(operand);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(1);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(operand);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testImplication() throws ParserException {
        final Formula formula = p.parse("a => c | d");
        final Formula left = f.variable("a");
        final Formula right = p.parse("c | d");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(left);
        assertThat(it.next()).isEqualTo(right);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(2);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(left, right);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testEquivalence() throws ParserException {
        final Formula formula = p.parse("a <=> c | d");
        final Formula left = f.variable("a");
        final Formula right = p.parse("c | d");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(left);
        assertThat(it.next()).isEqualTo(right);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(2);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(left, right);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testAnd() throws ParserException {
        final Formula formula = p.parse("a & (c | d) & ~e");
        final Formula op1 = p.parse("a");
        final Formula op2 = p.parse("c | d");
        final Formula op3 = p.parse("~e");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(op1);
        assertThat(it.next()).isEqualTo(op2);
        assertThat(it.next()).isEqualTo(op3);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(3);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(op1, op2, op3);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testOr() throws ParserException {
        final Formula formula = p.parse("a | (c & d) | ~e");
        final Formula op1 = p.parse("a");
        final Formula op2 = p.parse("c & d");
        final Formula op3 = p.parse("~e");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.next()).isEqualTo(op1);
        assertThat(it.next()).isEqualTo(op2);
        assertThat(it.next()).isEqualTo(op3);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
        assertThat(formula.stream().count()).isEqualTo(3);
        assertThat(formula.stream().collect(Collectors.toList())).containsExactly(op1, op2, op3);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testPBC() throws ParserException {
        final Formula formula = new PseudoBooleanParser(f).parse("3*a + 4*b + 5*c <= 8");
        final Iterator<Formula> it = formula.iterator();
        assertThat(it.hasNext()).isFalse();
        assertThat(formula.stream().count()).isEqualTo(0);
        assertThatThrownBy(() -> formula.iterator().next()).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> formula.iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
    }
}
