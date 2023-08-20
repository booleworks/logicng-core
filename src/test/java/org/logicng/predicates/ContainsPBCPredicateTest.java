// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.RandomTag;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.io.parsers.ParserException;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

public class ContainsPBCPredicateTest extends TestWithFormulaContext {

    private final ContainsPBCPredicate predicate = ContainsPBCPredicate.get();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        assertThat(_c.f.falsum().holds(predicate)).isFalse();
        assertThat(_c.f.verum().holds(predicate)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.a.holds(predicate)).isFalse();
        assertThat(_c.na.holds(predicate)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        assertThat(_c.f.parse("~a").holds(predicate)).isFalse();
        assertThat(_c.f.parse("~(a | b)").holds(predicate)).isFalse();

        assertThat(_c.f.parse("~(a | (a + b = 3))").holds(predicate)).isTrue();
        assertThat(_c.f.parse("~(a & ~(a + b = 3))").holds(predicate)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testMixed(final FormulaContext _c) throws ParserException {
        assertThat(_c.f.parse("a => b").holds(predicate)).isFalse();
        assertThat(_c.f.parse("a <=> b").holds(predicate)).isFalse();
        assertThat(_c.f.parse("a => (b | c & ~(e | d))").holds(predicate)).isFalse();
        assertThat(_c.f.parse("a <=> (b | c & ~(e | d))").holds(predicate)).isFalse();

        assertThat(_c.f.parse("a => (3*a + ~b <= 4)").holds(predicate)).isTrue();
        assertThat(_c.f.parse("(3*a + ~b <= 4) <=> b").holds(predicate)).isTrue();
        assertThat(_c.f.parse("a => (b | c & (3*a + ~b <= 4) & ~(e | d))").holds(predicate)).isTrue();
        assertThat(_c.f.parse("a <=> (b | c & ~(e | (3*a + ~b <= 4) | d))").holds(predicate)).isTrue();
        assertThat(_c.f.parse("3*a + ~b <= 4").holds(predicate)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void randomWithoutPBCs(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, FormulaRandomizerConfig.builder().numVars(10).weightPbc(0).seed(i * 42).build());
            final Formula formula = randomizer.formula(5);
            assertThat(formula.holds(predicate)).isFalse();
        }
    }
}
