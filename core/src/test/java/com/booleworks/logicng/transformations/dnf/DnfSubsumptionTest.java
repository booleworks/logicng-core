// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.dnf;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

public class DnfSubsumptionTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleDNFSubsumption(final FormulaContext _c) throws ParserException {
        final DnfSubsumption s = new DnfSubsumption(_c.f);

        assertThat(s.apply(_c.p.parse("$false"))).isEqualTo(_c.p.parse("$false"));
        assertThat(s.apply(_c.p.parse("$true"))).isEqualTo(_c.p.parse("$true"));
        assertThat(s.apply(_c.p.parse("a"))).isEqualTo(_c.p.parse("a"));
        assertThat(s.apply(_c.p.parse("~a"))).isEqualTo(_c.p.parse("~a"));
        assertThat(s.apply(_c.p.parse("a | b | c"))).isEqualTo(_c.p.parse("a | b | c"));
        assertThat(s.apply(_c.p.parse("a & b & c"))).isEqualTo(_c.p.parse("a & b & c"));
        assertThat(s.apply(_c.p.parse("a | (a & b)"))).isEqualTo(_c.p.parse("a"));
        assertThat(s.apply(_c.p.parse("(a & b) | (a & b & c)"))).isEqualTo(_c.p.parse("a & b"));
        assertThat(s.apply(_c.p.parse("a | (a & b) | (a & b & c)"))).isEqualTo(_c.p.parse("a"));
        assertThat(s.apply(_c.p.parse("a | (a & b) | b"))).isEqualTo(_c.p.parse("a | b"));
        assertThat(s.apply(_c.p.parse("a | (a & b) | c | (c & b)"))).isEqualTo(_c.p.parse("a | c"));
        assertThat(s.apply(_c.p.parse("(a & b) | (a & c) | (a & b & c)"))).isEqualTo(_c.p.parse("(a & b) | (a & c)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLargeDNFSubsumption(final FormulaContext _c) throws ParserException {
        final DnfSubsumption s = new DnfSubsumption(_c.f);

        assertThat(s.apply(_c.p.parse("(a & b & c & d) | (a & b & c & e) | (a & b & c)")))
                .isEqualTo(_c.p.parse("(a & b & c)"));
        assertThat(s.apply(_c.p.parse("(a & b) | (a & c) | (a & b & c) | (a & ~b & c) | (a & b & ~c) | (b & c)")))
                .isEqualTo(_c.p.parse("(a & b) | (a & c) | (b & c)"));
        assertThat(s.apply(_c.p.parse("(a & b) | (a & c) | (a & b & c) | (a & ~b & c) | (a & b & ~c) | (b & c)")))
                .isEqualTo(_c.p.parse("(a & b) | (a & c) | (b & c)"));
        assertThat(s.apply(_c.p.parse("a | ~b | (c & d) | (~a & ~b & ~c) | (b & c & d) | (a & b & c & d)")))
                .isEqualTo(_c.p.parse("a | ~b | (c & d)"));
        assertThat(s.apply(_c.p.parse("(a & b & c & d & e & f & g) | (b & d & f) | (a & c & e & g)")))
                .isEqualTo(_c.p.parse("(b & d & f) | (a & c & e & g)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @LongRunningTag
    public void testEvenLargerFormulas(final FormulaContext _c) throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readFormula(_c.f, "../test_files/formulas/small_formulas.txt");
        int count = 10; // test only first 10 formulas
        for (final Formula op : formula) {
            if (count == 0) {
                break;
            }
            final Formula dnf = op.transform(new DnfFactorization(_c.f));
            final Formula subsumed = dnf.transform(new DnfSubsumption(_c.f));
            assertThat(_c.f.equivalence(dnf, subsumed).holds(new TautologyPredicate(_c.f))).isTrue();
            assertThat(dnf.numberOfOperands() > subsumed.numberOfOperands()).isTrue();
            count--;
        }
    }

    @Test
    public void testNotInDNF() {
        final FormulaFactory f = FormulaFactory.caching();
        final DnfSubsumption s = new DnfSubsumption(f);
        assertThatThrownBy(() -> s.apply(f.parse("a => b"))).isInstanceOf(IllegalArgumentException.class);
    }
}
