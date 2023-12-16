// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TermPredicateTest extends TestWithFormulaContext {

    private final TermPredicate mintermPredicate = TermPredicate.minterm();
    private final TermPredicate maxtermPredicate = TermPredicate.maxterm();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testMintermPredicate(final FormulaContext _c) {
        assertThat(_c.f.verum().holds(mintermPredicate)).isTrue();
        assertThat(_c.f.falsum().holds(mintermPredicate)).isTrue();
        assertThat(_c.a.holds(mintermPredicate)).isTrue();
        assertThat(_c.na.holds(mintermPredicate)).isTrue();
        assertThat(_c.and1.holds(mintermPredicate)).isTrue();
        assertThat(_c.or1.holds(mintermPredicate)).isFalse();
        assertThat(_c.or3.holds(mintermPredicate)).isFalse();
        assertThat(_c.f.or(_c.and1, _c.and2, _c.a, _c.ny).holds(mintermPredicate)).isFalse();
        assertThat(_c.pbc1.holds(mintermPredicate)).isFalse();
        assertThat(_c.and3.holds(mintermPredicate)).isFalse();
        assertThat(_c.imp1.holds(mintermPredicate)).isFalse();
        assertThat(_c.eq1.holds(mintermPredicate)).isFalse();
        assertThat(_c.not1.holds(mintermPredicate)).isFalse();
        assertThat(_c.not2.holds(mintermPredicate)).isFalse();
        assertThat(_c.f.or(_c.and1, _c.eq1).holds(mintermPredicate)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testMaxtermPredicate(final FormulaContext _c) {
        assertThat(_c.f.verum().holds(maxtermPredicate)).isTrue();
        assertThat(_c.f.falsum().holds(maxtermPredicate)).isTrue();
        assertThat(_c.a.holds(maxtermPredicate)).isTrue();
        assertThat(_c.na.holds(maxtermPredicate)).isTrue();
        assertThat(_c.and1.holds(maxtermPredicate)).isFalse();
        assertThat(_c.or1.holds(maxtermPredicate)).isTrue();
        assertThat(_c.or3.holds(maxtermPredicate)).isFalse();
        assertThat(_c.f.or(_c.and1, _c.and2, _c.a, _c.ny).holds(maxtermPredicate)).isFalse();
        assertThat(_c.pbc1.holds(maxtermPredicate)).isFalse();
        assertThat(_c.and3.holds(maxtermPredicate)).isFalse();
        assertThat(_c.imp1.holds(maxtermPredicate)).isFalse();
        assertThat(_c.eq1.holds(maxtermPredicate)).isFalse();
        assertThat(_c.not1.holds(maxtermPredicate)).isFalse();
        assertThat(_c.not2.holds(maxtermPredicate)).isFalse();
        assertThat(_c.f.or(_c.and1, _c.eq1).holds(maxtermPredicate)).isFalse();
    }
}
