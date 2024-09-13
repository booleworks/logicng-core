// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

public class CNFSubsumptionTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleCNFSubsumption(final FormulaContext _c) throws ParserException {
        final CNFSubsumption s = new CNFSubsumption(_c.f);
        assertThat(s.apply(_c.p.parse("$false"))).isEqualTo(_c.p.parse("$false"));
        assertThat(s.apply(_c.p.parse("$true"))).isEqualTo(_c.p.parse("$true"));
        assertThat(s.apply(_c.p.parse("a"))).isEqualTo(_c.p.parse("a"));
        assertThat(s.apply(_c.p.parse("~a"))).isEqualTo(_c.p.parse("~a"));
        assertThat(s.apply(_c.p.parse("a | b | c"))).isEqualTo(_c.p.parse("a | b | c"));
        assertThat(s.apply(_c.p.parse("a & b & c"))).isEqualTo(_c.p.parse("a & b & c"));
        assertThat(s.apply(_c.p.parse("a & (a | b)"))).isEqualTo(_c.p.parse("a"));
        assertThat(s.apply(_c.p.parse("(a | b) & (a | b | c)"))).isEqualTo(_c.p.parse("a | b"));
        assertThat(s.apply(_c.p.parse("a & (a | b) & (a | b | c)"))).isEqualTo(_c.p.parse("a"));
        assertThat(s.apply(_c.p.parse("a & (a | b) & b"))).isEqualTo(_c.p.parse("a & b"));
        assertThat(s.apply(_c.p.parse("a & (a | b) & c & (c | b)"))).isEqualTo(_c.p.parse("a & c"));
        assertThat(s.apply(_c.p.parse("(a | b) & (a | c) & (a | b | c)")))
                .isEqualTo(_c.p.parse("(a | b) & (a | c)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLargeCNFSubsumption(final FormulaContext _c) throws ParserException {
        final CNFSubsumption s = new CNFSubsumption(_c.f);
        assertThat(s.apply(_c.p.parse("(a | b | c | d) & (a | b | c | e) & (a | b | c)")))
                .isEqualTo(_c.p.parse("(a | b | c)"));
        Assertions
                .assertThat(
                        s.apply(_c.p.parse("(a | b) & (a | c) & (a | b | c) & (a | ~b | c) & (a | b | ~c) & (b | c)")))
                .isEqualTo(_c.p.parse("(a | b) & (a | c) & (b | c)"));
        Assertions
                .assertThat(
                        s.apply(_c.p.parse("(a | b) & (a | c) & (a | b | c) & (a | ~b | c) & (a | b | ~c) & (b | c)")))
                .isEqualTo(_c.p.parse("(a | b) & (a | c) & (b | c)"));
        assertThat(s.apply(_c.p.parse("a & ~b & (c | d) & (~a | ~b | ~c) & (b | c | d) & (a | b | c | d)")))
                .isEqualTo(_c.p.parse("a & ~b & (c | d)"));
        assertThat(s.apply(_c.p.parse("(a | b | c | d | e | f | g) & (b | d | f) & (a | c | e | g)")))
                .isEqualTo(_c.p.parse("(b | d | f) & (a | c | e | g)"));
    }

    @Test
    public void testNotInCNF() {
        final FormulaFactory f = FormulaFactory.caching();
        final CNFSubsumption s = new CNFSubsumption(f);
        assertThatThrownBy(() -> s.apply(f.parse("a => b"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @LongRunningTag
    public void testEvenLargerFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        final Formula cnf = formula.transform(new CNFFactorization(f));
        final Formula subsumed = cnf.transform(new CNFSubsumption(f));
        assertThat(f.equivalence(cnf, subsumed).holds(new TautologyPredicate(f))).isTrue();
        assertThat(cnf.numberOfOperands() > subsumed.numberOfOperands()).isTrue();
    }
}
