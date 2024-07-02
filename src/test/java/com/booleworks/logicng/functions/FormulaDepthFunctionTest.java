// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.formulas.implementation.noncaching.NonCachingFormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

public class FormulaDepthFunctionTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtoms(final FormulaContext _c) {
        final FormulaDepthFunction df = new FormulaDepthFunction(_c.f);

        assertThat(_c.verum.apply(df)).isEqualTo(0);
        assertThat(_c.falsum.apply(df)).isEqualTo(0);
        assertThat(_c.a.apply(df)).isEqualTo(0);
        assertThat(_c.na.apply(df)).isEqualTo(0);
        assertThat(_c.pbc1.apply(df)).isEqualTo(0);
        assertThat(_c.pbc2.apply(df)).isEqualTo(0);
        assertThat(_c.pbc3.apply(df)).isEqualTo(0);
        assertThat(_c.pbc4.apply(df)).isEqualTo(0);
        assertThat(_c.pbc5.apply(df)).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testDeepFormulas(final FormulaContext _c) {
        final FormulaDepthFunction df = new FormulaDepthFunction(_c.f);

        assertThat(_c.and1.apply(df)).isEqualTo(1);
        assertThat(_c.and2.apply(df)).isEqualTo(1);
        assertThat(_c.and3.apply(df)).isEqualTo(2);
        assertThat(_c.or1.apply(df)).isEqualTo(1);
        assertThat(_c.or2.apply(df)).isEqualTo(1);
        assertThat(_c.or3.apply(df)).isEqualTo(2);
        assertThat(_c.not1.apply(df)).isEqualTo(2);
        assertThat(_c.not2.apply(df)).isEqualTo(2);
        assertThat(_c.imp1.apply(df)).isEqualTo(1);
        assertThat(_c.imp2.apply(df)).isEqualTo(1);
        assertThat(_c.imp3.apply(df)).isEqualTo(2);
        assertThat(_c.imp4.apply(df)).isEqualTo(2);
        assertThat(_c.eq1.apply(df)).isEqualTo(1);
        assertThat(_c.eq2.apply(df)).isEqualTo(1);
        assertThat(_c.eq3.apply(df)).isEqualTo(2);
        assertThat(_c.eq4.apply(df)).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testDeeperFormulas(final FormulaContext _c) {
        final FormulaDepthFunction df = new FormulaDepthFunction(_c.f);

        Formula formula = _c.pbc1;
        for (int i = 0; i < 10; i++) {
            final Variable var = _c.f.variable("X" + i);
            formula = i % 2 == 0 ? _c.f.or(formula, var) : _c.f.and(formula, var);
        }
        assertThat(formula.apply(df)).isEqualTo(10);
    }

    @Test
    public void testCacheCachingFF() throws ParserException {
        final CachingFormulaFactory f = FormulaFactory.caching();
        final FormulaDepthFunction df = new FormulaDepthFunction(f);

        final Formula formula = f.parse("A & B | C");
        final Map<Formula, Integer> cache = f.getFunctionCacheForType(FunctionCacheEntry.DEPTH);
        assertThat(cache.get(formula)).isNull();
        assertThat(formula.apply(df)).isEqualTo(2);
        assertThat(cache.get(formula)).isEqualTo(2);
        assertThat(cache.get(f.variable("A"))).isEqualTo(0);
        cache.put(formula, 3);
        assertThat(formula.apply(df)).isEqualTo(3);
    }

    @Test
    public void testCacheCachingOwnCache() throws ParserException {
        final CachingFormulaFactory f = FormulaFactory.caching();
        final Map<Formula, Integer> cache = new HashMap<>();
        final FormulaDepthFunction df = new FormulaDepthFunction(f, cache);

        final Formula formula = f.parse("A & B | C");
        final Map<Formula, Integer> ffCache = f.getFunctionCacheForType(FunctionCacheEntry.DEPTH);

        assertThat(cache.get(formula)).isNull();
        assertThat(formula.apply(df)).isEqualTo(2);
        assertThat(cache.get(formula)).isEqualTo(2);
        assertThat(cache.get(f.variable("A"))).isEqualTo(0);
        cache.put(formula, 3);
        assertThat(formula.apply(df)).isEqualTo(3);

        assertThat(ffCache.get(formula)).isNull();
        assertThat(ffCache.get(f.variable("A"))).isNull();
        ffCache.put(formula, 5);
        assertThat(formula.apply(df)).isEqualTo(3);
    }

    @Test
    public void testCacheNonCaching() throws ParserException {
        final NonCachingFormulaFactory f = FormulaFactory.nonCaching();
        final Map<Formula, Integer> cache = new HashMap<>();
        final FormulaDepthFunction df = new FormulaDepthFunction(f, cache);

        final Formula formula = f.parse("A & B | C");
        assertThat(cache.get(formula)).isNull();
        assertThat(formula.apply(df)).isEqualTo(2);
        assertThat(cache.get(formula)).isEqualTo(2);
        assertThat(cache.get(f.variable("A"))).isEqualTo(0);
        cache.put(formula, 3);
        assertThat(formula.apply(df)).isEqualTo(3);
    }
}
