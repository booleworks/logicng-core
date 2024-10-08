// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.api.Test;

public class OccurrenceOrderingTest {

    private final MinToMaxOrdering min2max = new MinToMaxOrdering();
    private final MaxToMinOrdering max2min = new MaxToMinOrdering();

    @Test
    public void testSimpleCasesMin2Max() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(min2max.getOrder(f, p.parse("$true"))).isEmpty();
        assertThat(min2max.getOrder(f, p.parse("$false"))).isEmpty();
        assertThat(min2max.getOrder(f, p.parse("A"))).containsExactly(f.variable("A"));
        assertThat(min2max.getOrder(f, p.parse("A => ~B"))).containsExactly(f.variable("A"),
                f.variable("B"));
        assertThat(min2max.getOrder(f, p.parse("A <=> ~B"))).containsExactly(f.variable("A"),
                f.variable("B"));
        assertThat(min2max.getOrder(f, p.parse("~(A <=> ~B)"))).containsExactly(f.variable("A"),
                f.variable("B"));
        assertThat(min2max.getOrder(f, p.parse("A | ~C | B | D"))).containsExactly(f.variable("A"),
                f.variable("C"), f.variable("B"), f.variable("D"));
        assertThat(min2max.getOrder(f, p.parse("A & ~C & B & D"))).containsExactly(f.variable("A"),
                f.variable("C"), f.variable("B"), f.variable("D"));
        assertThat(min2max.getOrder(f, p.parse("A + C + B + D < 2"))).containsExactly(f.variable("A"),
                f.variable("C"), f.variable("B"), f.variable("D"));
    }

    @Test
    public void testSimpleCasesMax2Min() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(max2min.getOrder(f, p.parse("$true"))).isEmpty();
        assertThat(max2min.getOrder(f, p.parse("$false"))).isEmpty();
        assertThat(max2min.getOrder(f, p.parse("A"))).containsExactly(f.variable("A"));
        assertThat(max2min.getOrder(f, p.parse("A => ~B"))).containsExactly(f.variable("A"),
                f.variable("B"));
        assertThat(max2min.getOrder(f, p.parse("A <=> ~B"))).containsExactly(f.variable("A"),
                f.variable("B"));
        assertThat(max2min.getOrder(f, p.parse("~(A <=> ~B)"))).containsExactly(f.variable("A"),
                f.variable("B"));
        assertThat(max2min.getOrder(f, p.parse("A | ~C | B | D"))).containsExactly(f.variable("A"),
                f.variable("C"), f.variable("B"), f.variable("D"));
        assertThat(max2min.getOrder(f, p.parse("A & ~C & B & D"))).containsExactly(f.variable("A"),
                f.variable("C"), f.variable("B"), f.variable("D"));
        assertThat(max2min.getOrder(f, p.parse("A + C + B + D < 2"))).containsExactly(f.variable("A"),
                f.variable("C"), f.variable("B"), f.variable("D"));
    }

    @Test
    public void testComplexFormulaMin2Max() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula =
                p.parse("(A => ~B) & ((A & C) | (D & ~C)) & (A | Y | X) & (Y <=> (X | (X + W + A + F < 1)))");

        assertThat(min2max.getOrder(f, formula)).containsExactly(
                f.variable("A"),
                f.variable("X"),
                f.variable("C"),
                f.variable("Y"),
                f.variable("B"),
                f.variable("D"),
                f.variable("W"),
                f.variable("F")
        );
    }

    @Test
    public void testComplexFormulaMax2Min() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula =
                p.parse("(A => ~B) & ((A & C) | (D & ~C)) & (A | Y | X) & (Y <=> (X | (X + W + A + F < 1)))");

        assertThat(max2min.getOrder(f, formula)).containsExactly(
                f.variable("B"),
                f.variable("D"),
                f.variable("W"),
                f.variable("F"),
                f.variable("C"),
                f.variable("Y"),
                f.variable("X"),
                f.variable("A")
        );
    }
}
