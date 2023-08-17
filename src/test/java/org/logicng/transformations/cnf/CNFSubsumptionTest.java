// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.logicng.LongRunningTag;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.readers.FormulaReader;
import org.logicng.predicates.satisfiability.TautologyPredicate;

import java.io.IOException;

public class CNFSubsumptionTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final PropositionalParser p = new PropositionalParser(this.f);
    private final CNFSubsumption s = new CNFSubsumption(f);

    @Test
    public void testNotInCNF() {
        assertThatThrownBy(() -> this.s.apply(this.p.parse("a => b"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSimpleCNFSubsumption() throws ParserException {
        assertThat(this.s.apply(this.p.parse("$false"))).isEqualTo(this.p.parse("$false"));
        assertThat(this.s.apply(this.p.parse("$true"))).isEqualTo(this.p.parse("$true"));
        assertThat(this.s.apply(this.p.parse("a"))).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("~a"))).isEqualTo(this.p.parse("~a"));
        assertThat(this.s.apply(this.p.parse("a | b | c"))).isEqualTo(this.p.parse("a | b | c"));
        assertThat(this.s.apply(this.p.parse("a & b & c"))).isEqualTo(this.p.parse("a & b & c"));
        assertThat(this.s.apply(this.p.parse("a & (a | b)"))).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | b | c)"))).isEqualTo(this.p.parse("a | b"));
        assertThat(this.s.apply(this.p.parse("a & (a | b) & (a | b | c)"))).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("a & (a | b) & b"))).isEqualTo(this.p.parse("a & b"));
        assertThat(this.s.apply(this.p.parse("a & (a | b) & c & (c | b)"))).isEqualTo(this.p.parse("a & c"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | c) & (a | b | c)"))).isEqualTo(this.p.parse("(a | b) & (a | c)"));
    }

    @Test
    public void testLargeCNFSubsumption() throws ParserException {
        assertThat(this.s.apply(this.p.parse("(a | b | c | d) & (a | b | c | e) & (a | b | c)"))).isEqualTo(this.p.parse("(a | b | c)"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | c) & (a | b | c) & (a | ~b | c) & (a | b | ~c) & (b | c)"))).isEqualTo(this.p.parse("(a | b) & (a | c) & (b | c)"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | c) & (a | b | c) & (a | ~b | c) & (a | b | ~c) & (b | c)"))).isEqualTo(this.p.parse("(a | b) & (a | c) & (b | c)"));
        assertThat(this.s.apply(this.p.parse("a & ~b & (c | d) & (~a | ~b | ~c) & (b | c | d) & (a | b | c | d)"))).isEqualTo(this.p.parse("a & ~b & (c | d)"));
        assertThat(this.s.apply(this.p.parse("(a | b | c | d | e | f | g) & (b | d | f) & (a | c | e | g)"))).isEqualTo(this.p.parse("(b | d | f) & (a | c | e | g)"));
    }

    @Test
    @LongRunningTag
    public void testEvenLargerFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
        final Formula cnf = formula.transform(new CNFFactorization(f));
        final Formula subsumed = cnf.transform(new CNFSubsumption(f));
        assertThat(f.equivalence(cnf, subsumed).holds(new TautologyPredicate(f))).isTrue();
        assertThat(cnf.numberOfOperands() > subsumed.numberOfOperands()).isTrue();
    }
}
