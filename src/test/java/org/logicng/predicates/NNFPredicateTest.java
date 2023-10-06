// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.TestWithFormulaContext;

public class NNFPredicateTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void test(final FormulaContext _c) {
        final NNFPredicate nnfPredicate = new NNFPredicate(_c.f);

        assertThat(_c.f.verum().holds(nnfPredicate)).isTrue();
        assertThat(_c.f.falsum().holds(nnfPredicate)).isTrue();
        assertThat(_c.a.holds(nnfPredicate)).isTrue();
        assertThat(_c.na.holds(nnfPredicate)).isTrue();
        assertThat(_c.or1.holds(nnfPredicate)).isTrue();
        assertThat(_c.and1.holds(nnfPredicate)).isTrue();
        assertThat(_c.and3.holds(nnfPredicate)).isTrue();
        assertThat(_c.f.and(_c.or1, _c.or2, _c.a, _c.ny).holds(nnfPredicate)).isTrue();
        assertThat(_c.f.and(_c.or1, _c.or2, _c.and1, _c.and2, _c.and3, _c.a, _c.ny).holds(nnfPredicate)).isTrue();
        assertThat(_c.or3.holds(nnfPredicate)).isTrue();
        assertThat(_c.pbc1.holds(nnfPredicate)).isFalse();
        assertThat(_c.imp1.holds(nnfPredicate)).isFalse();
        assertThat(_c.eq1.holds(nnfPredicate)).isFalse();
        assertThat(_c.not1.holds(nnfPredicate)).isFalse();
        assertThat(_c.not2.holds(nnfPredicate)).isFalse();
        assertThat(_c.f.and(_c.or1, _c.f.not(_c.or2), _c.a, _c.ny).holds(nnfPredicate)).isFalse();
        assertThat(_c.f.and(_c.or1, _c.eq1).holds(nnfPredicate)).isFalse();
        assertThat(_c.f.and(_c.or1, _c.imp1, _c.and1).holds(nnfPredicate)).isFalse();
        assertThat(_c.f.and(_c.or1, _c.pbc1, _c.and1).holds(nnfPredicate)).isFalse();
    }
}
