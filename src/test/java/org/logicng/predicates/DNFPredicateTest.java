// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;

public class DNFPredicateTest extends TestWithExampleFormulas {

    private final DNFPredicate dnfPredicate = new DNFPredicate();

    @Test
    public void test() {
        assertThat(this.f.verum().holds(this.dnfPredicate)).isTrue();
        assertThat(this.f.falsum().holds(this.dnfPredicate)).isTrue();
        assertThat(this.A.holds(this.dnfPredicate)).isTrue();
        assertThat(this.NA.holds(this.dnfPredicate)).isTrue();
        assertThat(this.AND1.holds(this.dnfPredicate)).isTrue();
        assertThat(this.OR1.holds(this.dnfPredicate)).isTrue();
        assertThat(this.OR3.holds(this.dnfPredicate)).isTrue();
        assertThat(this.f.or(this.AND1, this.AND2, this.A, this.NY).holds(this.dnfPredicate)).isTrue();
        assertThat(this.PBC1.holds(this.dnfPredicate)).isFalse();
        assertThat(this.AND3.holds(this.dnfPredicate)).isFalse();
        assertThat(this.IMP1.holds(this.dnfPredicate)).isFalse();
        assertThat(this.EQ1.holds(this.dnfPredicate)).isFalse();
        assertThat(this.NOT1.holds(this.dnfPredicate)).isFalse();
        assertThat(this.NOT2.holds(this.dnfPredicate)).isFalse();
        assertThat(this.f.or(this.AND1, this.EQ1).holds(this.dnfPredicate)).isFalse();
    }
}
