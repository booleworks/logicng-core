// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.formulas.cache.PredicateCacheEntry.IS_CNF;
import static org.logicng.formulas.cache.PredicateCacheEntry.IS_DNF;

import org.junit.jupiter.api.Test;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.cache.CacheEntry;

import java.util.Arrays;

public class FormulaTest {

    private enum MyOwnCacheKey implements CacheEntry {
        MYKEY1("My Key 1"),
        MYKEY2("My Key 2");

        private final String description;

        MyOwnCacheKey(final String description) {
            this.description = description;
        }

        @Override
        public String description() {
            return "MyOwnCacheKey{description=" + description + "}";
        }
    }

    @Test
    public void testPredicateCache() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.not(f.and(f.variable("a"), f.variable("b")));
        formula.setPredicateCacheEntry(IS_CNF, false);
        formula.setPredicateCacheEntry(IS_DNF, Tristate.UNDEF);
        assertThat(formula.predicateCacheEntry(IS_CNF)).isEqualTo(Tristate.FALSE);
        assertThat(formula.predicateCacheEntry(IS_DNF)).isEqualTo(Tristate.UNDEF);
    }

    @Test
    public void testFunctionCache() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.not(f.and(f.variable("a"), f.variable("b")));
        formula.setFunctionCacheEntry(MyOwnCacheKey.MYKEY1, "key1");
        formula.setFunctionCacheEntry(MyOwnCacheKey.MYKEY2, "key2");
        assertThat(MyOwnCacheKey.MYKEY1.description).isEqualTo("My Key 1");
        assertThat(formula.functionCacheEntry(MyOwnCacheKey.MYKEY1)).isEqualTo("key1");
        assertThat(formula.functionCacheEntry(MyOwnCacheKey.MYKEY2)).isEqualTo("key2");
    }

    @Test
    public void testFType() {
        assertThat(FType.valueOf("AND")).isEqualTo(FType.AND);
        assertThat(Arrays.asList(FType.values()).contains(FType.valueOf("PBC"))).isTrue();
        assertThat(FType.values().length).isEqualTo(9);
    }

    @Test
    public void testCType() {
        assertThat(CType.valueOf("EQ")).isEqualTo(CType.EQ);
        assertThat(CType.valueOf("LE")).isEqualTo(CType.LE);
        assertThat(Arrays.asList(CType.values()).contains(CType.valueOf("GT"))).isTrue();
        assertThat(CType.values().length).isEqualTo(5);
    }
}
