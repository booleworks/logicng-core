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

public class BFSOrderingTest {

    private final BFSOrdering ordering = new BFSOrdering();

    @Test
    public void testSimpleCases() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(ordering.getOrder(f, p.parse("$true"))).isEmpty();
        assertThat(ordering.getOrder(f, p.parse("$false"))).isEmpty();
        assertThat(ordering.getOrder(f, p.parse("A"))).containsExactly(f.variable("A"));
        assertThat(ordering.getOrder(f, p.parse("A => ~B"))).containsExactly(f.variable("A"), f.variable("B"));
        assertThat(ordering.getOrder(f, p.parse("A <=> ~B"))).containsExactly(f.variable("A"), f.variable("B"));
        assertThat(ordering.getOrder(f, p.parse("~(A <=> ~B)"))).containsExactly(f.variable("A"), f.variable("B"));
        assertThat(ordering.getOrder(f, p.parse("A | ~C | B | D"))).containsExactly(f.variable("A"), f.variable("B"),
                f.variable("D"), f.variable("C"));
        assertThat(ordering.getOrder(f, p.parse("A & ~C & B & D"))).containsExactly(f.variable("A"), f.variable("B"),
                f.variable("D"), f.variable("C"));
        assertThat(ordering.getOrder(f, p.parse("A + C + B + D < 2"))).containsExactly(f.variable("A"), f.variable("C"),
                f.variable("B"), f.variable("D"));
    }

    @Test
    public void testComplexFormula() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula =
                p.parse("(A => ~B) & ((A & C) | (D & ~C)) & (A | Y | X) & (Y <=> (X | (W + A + F < 1)))");

        assertThat(ordering.getOrder(f, formula)).containsExactly(
                f.variable("A"),
                f.variable("Y"),
                f.variable("X"),
                f.variable("B"),
                f.variable("C"),
                f.variable("D"),
                f.variable("W"),
                f.variable("F")
        );
    }
}
