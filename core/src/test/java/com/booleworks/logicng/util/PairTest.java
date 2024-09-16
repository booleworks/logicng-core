// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PairTest {

    private final Pair<String, Integer> pair1 = new Pair<>("abc", 12);
    private final Pair<String, Integer> pair2 = new Pair<>("cde", 12);
    private final Pair<String, Integer> pair3 = new Pair<>("cde", 42);

    @Test
    public void testGetters() {
        assertThat(pair1.getFirst()).isEqualTo("abc");
        assertThat(pair2.getFirst()).isEqualTo("cde");
        assertThat(pair3.getFirst()).isEqualTo("cde");
        assertThat((int) pair1.getSecond()).isEqualTo(12);
        assertThat((int) pair2.getSecond()).isEqualTo(12);
        assertThat((int) pair3.getSecond()).isEqualTo(42);
    }

    @Test
    public void testHashCode() {
        assertThat(pair1.hashCode()).isEqualTo(pair1.hashCode());
        assertThat(new Pair<>("abc", 12).hashCode()).isEqualTo(pair1.hashCode());
    }

    @Test
    public void testEquals() {
        assertThat(pair1.equals(pair1)).isTrue();
        assertThat(pair1.equals(new Pair<>("abc", 12))).isTrue();
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

}
