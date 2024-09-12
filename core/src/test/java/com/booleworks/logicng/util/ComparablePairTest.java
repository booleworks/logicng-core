// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ComparablePairTest {

    private final ComparablePair<String, Integer> pair1 = new ComparablePair<>("abc", 12);
    private final ComparablePair<String, Integer> pair2 = new ComparablePair<>("cde", 12);
    private final ComparablePair<String, Integer> pair3 = new ComparablePair<>("cde", 42);

    @Test
    public void testGetters() {
        assertThat(pair1.first()).isEqualTo("abc");
        assertThat(pair2.first()).isEqualTo("cde");
        assertThat(pair3.first()).isEqualTo("cde");
        assertThat((int) pair1.second()).isEqualTo(12);
        assertThat((int) pair2.second()).isEqualTo(12);
        assertThat((int) pair3.second()).isEqualTo(42);
    }

    @Test
    public void testHashCode() {
        assertThat(pair1.hashCode()).isEqualTo(pair1.hashCode());
        assertThat(new ComparablePair<>("abc", 12).hashCode()).isEqualTo(pair1.hashCode());
    }

    @Test
    public void testEquals() {
        assertThat(pair1.equals(pair1)).isTrue();
        assertThat(pair1.equals(new ComparablePair<>("abc", 12))).isTrue();
        assertThat(pair1.equals(pair2)).isFalse();
        assertThat(pair2.equals(pair3)).isFalse();
        assertThat(pair1.equals(pair3)).isFalse();
        assertThat(pair1.equals("String")).isFalse();
        assertThat(pair1.equals(null)).isFalse();
    }

    @Test
    public void testToString() {
        assertThat(pair1.toString()).isEqualTo("<abc, 12>");
        assertThat(pair2.toString()).isEqualTo("<cde, 12>");
        assertThat(pair3.toString()).isEqualTo("<cde, 42>");
    }

    @Test
    public void testCompare() {
        assertThat((int) Math.signum(pair1.compareTo(pair1))).isEqualTo(0);
        assertThat((int) Math.signum(pair1.compareTo(pair2))).isEqualTo(-1);
        assertThat((int) Math.signum(pair3.compareTo(pair1))).isEqualTo(1);
        assertThat((int) Math.signum(pair2.compareTo(pair3))).isEqualTo(-1);
    }
}
