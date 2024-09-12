// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.cache.PredicateCacheEntry;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

public class CNFPredicateTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void test(final FormulaContext _c) {
        final CNFPredicate cnfPredicate = new CNFPredicate(_c.f);

        assertThat(_c.f.verum().holds(cnfPredicate)).isTrue();
        assertThat(_c.f.falsum().holds(cnfPredicate)).isTrue();
        assertThat(_c.a.holds(cnfPredicate)).isTrue();
        assertThat(_c.na.holds(cnfPredicate)).isTrue();
        assertThat(_c.or1.holds(cnfPredicate)).isTrue();
        assertThat(_c.and1.holds(cnfPredicate)).isTrue();
        assertThat(_c.and3.holds(cnfPredicate)).isTrue();
        assertThat(_c.f.and(_c.or1, _c.or2, _c.a, _c.ny).holds(cnfPredicate)).isTrue();
        assertThat(_c.pbc1.holds(cnfPredicate)).isFalse();
        assertThat(_c.or3.holds(cnfPredicate)).isFalse();
        assertThat(_c.imp1.holds(cnfPredicate)).isFalse();
        assertThat(_c.eq1.holds(cnfPredicate)).isFalse();
        assertThat(_c.not1.holds(cnfPredicate)).isFalse();
        assertThat(_c.not2.holds(cnfPredicate)).isFalse();
        assertThat(_c.f.and(_c.or1, _c.eq1).holds(cnfPredicate)).isFalse();
    }

    @Test
    public void testWithCachingFF() throws ParserException {
        final CachingFormulaFactory f = FormulaFactory.caching();
        final CNFPredicate cnfPredicate = new CNFPredicate(f);
        final Formula formula = f.parse("(a => b) & b & ~c");
        assertThat(formula.holds(cnfPredicate)).isFalse();
        assertThat(f.getPredicateCacheForType(PredicateCacheEntry.IS_CNF).get(formula)).isEqualTo(false);
    }

    @Test
    public void testWithNonCaching() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final Map<Formula, Boolean> cache = new HashMap<>();
        final CNFPredicate cnfPredicate = new CNFPredicate(f, cache);
        final Formula formula = f.parse("(a => b) & b & ~c");
        assertThat(formula.holds(cnfPredicate)).isFalse();
        assertThat(cache.get(formula)).isEqualTo(false);
    }
}
