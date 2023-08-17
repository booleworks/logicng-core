// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;

public class NNFPredicateTest extends TestWithExampleFormulas {

    private final NNFPredicate nnfPredicate = new NNFPredicate();

    @Test
    public void test() {
        assertThat(this.f.verum().holds(this.nnfPredicate)).isTrue();
        assertThat(this.f.falsum().holds(this.nnfPredicate)).isTrue();
        assertThat(this.A.holds(this.nnfPredicate)).isTrue();
        assertThat(this.NA.holds(this.nnfPredicate)).isTrue();
        assertThat(this.OR1.holds(this.nnfPredicate)).isTrue();
        assertThat(this.AND1.holds(this.nnfPredicate)).isTrue();
        assertThat(this.AND3.holds(this.nnfPredicate)).isTrue();
        assertThat(this.f.and(this.OR1, this.OR2, this.A, this.NY).holds(this.nnfPredicate)).isTrue();
        assertThat(this.f.and(this.OR1, this.OR2, this.AND1, this.AND2, this.AND3, this.A, this.NY).holds(this.nnfPredicate)).isTrue();
        assertThat(this.OR3.holds(this.nnfPredicate)).isTrue();
        assertThat(this.PBC1.holds(this.nnfPredicate)).isFalse();
        assertThat(this.IMP1.holds(this.nnfPredicate)).isFalse();
        assertThat(this.EQ1.holds(this.nnfPredicate)).isFalse();
        assertThat(this.NOT1.holds(this.nnfPredicate)).isFalse();
        assertThat(this.NOT2.holds(this.nnfPredicate)).isFalse();
        assertThat(this.f.and(this.OR1, this.f.not(this.OR2), this.A, this.NY).holds(this.nnfPredicate)).isFalse();
        assertThat(this.f.and(this.OR1, this.EQ1).holds(this.nnfPredicate)).isFalse();
        assertThat(this.f.and(this.OR1, this.IMP1, this.AND1).holds(this.nnfPredicate)).isFalse();
        assertThat(this.f.and(this.OR1, this.PBC1, this.AND1).holds(this.nnfPredicate)).isFalse();
    }
}
