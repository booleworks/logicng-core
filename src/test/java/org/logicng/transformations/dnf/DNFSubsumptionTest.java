// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.readers.FormulaReader;
import org.logicng.predicates.satisfiability.TautologyPredicate;

import java.io.IOException;

public class DNFSubsumptionTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final PropositionalParser p = new PropositionalParser(this.f);
    private final DNFSubsumption s = new DNFSubsumption(f);

    @Test
    public void testNotInDNF() {
        assertThatThrownBy(() -> this.s.apply(this.p.parse("a => b"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSimpleDNFSubsumption() throws ParserException {
        assertThat(this.s.apply(this.p.parse("$false"))).isEqualTo(this.p.parse("$false"));
        assertThat(this.s.apply(this.p.parse("$true"))).isEqualTo(this.p.parse("$true"));
        assertThat(this.s.apply(this.p.parse("a"))).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("~a"))).isEqualTo(this.p.parse("~a"));
        assertThat(this.s.apply(this.p.parse("a | b | c"))).isEqualTo(this.p.parse("a | b | c"));
        assertThat(this.s.apply(this.p.parse("a & b & c"))).isEqualTo(this.p.parse("a & b & c"));
        assertThat(this.s.apply(this.p.parse("a | (a & b)"))).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("(a & b) | (a & b & c)"))).isEqualTo(this.p.parse("a & b"));
        assertThat(this.s.apply(this.p.parse("a | (a & b) | (a & b & c)"))).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("a | (a & b) | b"))).isEqualTo(this.p.parse("a | b"));
        assertThat(this.s.apply(this.p.parse("a | (a & b) | c | (c & b)"))).isEqualTo(this.p.parse("a | c"));
        assertThat(this.s.apply(this.p.parse("(a & b) | (a & c) | (a & b & c)"))).isEqualTo(this.p.parse("(a & b) | (a & c)"));
    }

    @Test
    public void testLargeDNFSubsumption() throws ParserException {
        assertThat(this.s.apply(this.p.parse("(a & b & c & d) | (a & b & c & e) | (a & b & c)"))).isEqualTo(this.p.parse("(a & b & c)"));
        assertThat(this.s.apply(this.p.parse("(a & b) | (a & c) | (a & b & c) | (a & ~b & c) | (a & b & ~c) | (b & c)"))).isEqualTo(this.p.parse("(a & b) | (a & c) | (b & c)"));
        assertThat(this.s.apply(this.p.parse("(a & b) | (a & c) | (a & b & c) | (a & ~b & c) | (a & b & ~c) | (b & c)"))).isEqualTo(this.p.parse("(a & b) | (a & c) | (b & c)"));
        assertThat(this.s.apply(this.p.parse("a | ~b | (c & d) | (~a & ~b & ~c) | (b & c & d) | (a & b & c & d)"))).isEqualTo(this.p.parse("a | ~b | (c & d)"));
        assertThat(this.s.apply(this.p.parse("(a & b & c & d & e & f & g) | (b & d & f) | (a & c & e & g)"))).isEqualTo(this.p.parse("(b & d & f) | (a & c & e & g)"));
    }

    @Test
    public void testEvenLargerFormulas() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/small_formulas.txt", f);
        int count = 10; // test only first 10 formulas
        for (final Formula op : formula) {
            if (count == 0) {
                break;
            }
            final Formula dnf = op.transform(new DNFFactorization(f));
            final Formula subsumed = dnf.transform(new DNFSubsumption(f));
            assertThat(f.equivalence(dnf, subsumed).holds(new TautologyPredicate(f))).isTrue();
            assertThat(dnf.numberOfOperands() > subsumed.numberOfOperands()).isTrue();
            count--;
        }
    }
}
