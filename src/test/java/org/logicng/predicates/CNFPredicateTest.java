// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.formulas.cache.PredicateCacheEntry.IS_CNF;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;

public class CNFPredicateTest extends TestWithExampleFormulas {

    private final CNFPredicate cnfPredicate = new CNFPredicate();

    @Test
    public void test() {
        assertThat(this.f.verum().holds(this.cnfPredicate)).isTrue();
        assertThat(this.f.falsum().holds(this.cnfPredicate)).isTrue();
        assertThat(this.A.holds(this.cnfPredicate)).isTrue();
        assertThat(this.NA.holds(this.cnfPredicate)).isTrue();
        assertThat(this.OR1.holds(this.cnfPredicate)).isTrue();
        assertThat(this.AND1.holds(this.cnfPredicate)).isTrue();
        assertThat(this.AND3.holds(this.cnfPredicate)).isTrue();
        assertThat(this.f.and(this.OR1, this.OR2, this.A, this.NY).holds(this.cnfPredicate)).isTrue();
        assertThat(this.PBC1.holds(this.cnfPredicate)).isFalse();
        assertThat(this.OR3.holds(this.cnfPredicate)).isFalse();
        assertThat(this.IMP1.holds(this.cnfPredicate)).isFalse();
        assertThat(this.EQ1.holds(this.cnfPredicate)).isFalse();
        assertThat(this.NOT1.holds(this.cnfPredicate)).isFalse();
        assertThat(this.NOT2.holds(this.cnfPredicate)).isFalse();
        assertThat(this.f.and(this.OR1, this.EQ1).holds(this.cnfPredicate)).isFalse();
    }

    @Test
    public void testAndClearedCache() {
        this.AND1.setPredicateCacheEntry(IS_CNF, null);
        assertThat(this.AND1.holds(this.cnfPredicate)).isTrue();
    }
}
