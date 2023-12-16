// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DNFPredicateTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void test(final FormulaContext _c) {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);

        assertThat(_c.f.verum().holds(dnfPredicate)).isTrue();
        assertThat(_c.f.falsum().holds(dnfPredicate)).isTrue();
        assertThat(_c.a.holds(dnfPredicate)).isTrue();
        assertThat(_c.na.holds(dnfPredicate)).isTrue();
        assertThat(_c.and1.holds(dnfPredicate)).isTrue();
        assertThat(_c.or1.holds(dnfPredicate)).isTrue();
        assertThat(_c.or3.holds(dnfPredicate)).isTrue();
        assertThat(_c.f.or(_c.and1, _c.and2, _c.a, _c.ny).holds(dnfPredicate)).isTrue();
        assertThat(_c.pbc1.holds(dnfPredicate)).isFalse();
        assertThat(_c.and3.holds(dnfPredicate)).isFalse();
        assertThat(_c.imp1.holds(dnfPredicate)).isFalse();
        assertThat(_c.eq1.holds(dnfPredicate)).isFalse();
        assertThat(_c.not1.holds(dnfPredicate)).isFalse();
        assertThat(_c.not2.holds(dnfPredicate)).isFalse();
        assertThat(_c.f.or(_c.and1, _c.eq1).holds(dnfPredicate)).isFalse();
    }
}
