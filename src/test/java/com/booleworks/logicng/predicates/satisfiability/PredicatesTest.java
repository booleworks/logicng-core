// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates.satisfiability;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class PredicatesTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrue(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);
        final var ctr = new ContradictionPredicate(_c.f);
        final var tau = new TautologyPredicate(_c.f);
        final var con = new ContingencyPredicate(_c.f);

        assertThat(_c.verum.holds(sat)).isTrue();
        assertThat(_c.verum.holds(ctr)).isFalse();
        assertThat(_c.verum.holds(tau)).isTrue();
        assertThat(_c.verum.holds(con)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFalse(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);
        final var ctr = new ContradictionPredicate(_c.f);
        final var tau = new TautologyPredicate(_c.f);
        final var con = new ContingencyPredicate(_c.f);

        assertThat(_c.falsum.holds(sat)).isFalse();
        assertThat(_c.falsum.holds(ctr)).isTrue();
        assertThat(_c.falsum.holds(tau)).isFalse();
        assertThat(_c.falsum.holds(con)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);
        final var ctr = new ContradictionPredicate(_c.f);
        final var tau = new TautologyPredicate(_c.f);
        final var con = new ContingencyPredicate(_c.f);

        assertThat(_c.a.holds(sat)).isTrue();
        assertThat(_c.a.holds(ctr)).isFalse();
        assertThat(_c.a.holds(tau)).isFalse();
        assertThat(_c.a.holds(con)).isTrue();
        assertThat(_c.na.holds(sat)).isTrue();
        assertThat(_c.na.holds(ctr)).isFalse();
        assertThat(_c.na.holds(tau)).isFalse();
        assertThat(_c.na.holds(con)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testOther(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);
        final var ctr = new ContradictionPredicate(_c.f);
        final var tau = new TautologyPredicate(_c.f);
        final var con = new ContingencyPredicate(_c.f);

        assertThat(_c.and1.holds(sat)).isTrue();
        assertThat(_c.and1.holds(ctr)).isFalse();
        assertThat(_c.and1.holds(tau)).isFalse();
        assertThat(_c.and1.holds(con)).isTrue();
        assertThat(_c.not2.holds(sat)).isTrue();
        assertThat(_c.not2.holds(ctr)).isFalse();
        assertThat(_c.not2.holds(tau)).isFalse();
        assertThat(_c.not2.holds(con)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTaut(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);
        final var ctr = new ContradictionPredicate(_c.f);
        final var tau = new TautologyPredicate(_c.f);
        final var con = new ContingencyPredicate(_c.f);

        final Formula taut = _c.f.or(_c.and1, _c.f.and(_c.na, _c.b), _c.f.and(_c.a, _c.nb), _c.f.and(_c.na, _c.nb));
        assertThat(taut.holds(sat)).isTrue();
        assertThat(taut.holds(ctr)).isFalse();
        assertThat(taut.holds(tau)).isTrue();
        assertThat(taut.holds(con)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCont(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);
        final var ctr = new ContradictionPredicate(_c.f);
        final var tau = new TautologyPredicate(_c.f);
        final var con = new ContingencyPredicate(_c.f);

        final Formula cont = _c.f.and(_c.or1, _c.f.or(_c.nx, _c.y), _c.f.or(_c.x, _c.ny), _c.f.or(_c.nx, _c.ny));
        assertThat(cont.holds(sat)).isFalse();
        assertThat(cont.holds(ctr)).isTrue();
        assertThat(cont.holds(tau)).isFalse();
        assertThat(cont.holds(con)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSat(final FormulaContext _c) {
        final var sat = new SATPredicate(_c.f);

        assertThat(_c.and1.holds(sat)).isTrue();
        assertThat(_c.and2.holds(sat)).isTrue();
        assertThat(_c.and3.holds(sat)).isTrue();
        assertThat(_c.or1.holds(sat)).isTrue();
        assertThat(_c.or2.holds(sat)).isTrue();
        assertThat(_c.or3.holds(sat)).isTrue();
        assertThat(_c.not1.holds(sat)).isTrue();
        assertThat(_c.not2.holds(sat)).isTrue();
        Assertions.assertThat(new PigeonHoleGenerator(_c.f).generate(1).holds(sat)).isFalse();
        Assertions.assertThat(new PigeonHoleGenerator(_c.f).generate(2).holds(sat)).isFalse();
        Assertions.assertThat(new PigeonHoleGenerator(_c.f).generate(3).holds(sat)).isFalse();
    }
}
