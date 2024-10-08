// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class LngBooleanVectorTest {

    @Test
    public void testVectorCreation() {
        final LngBooleanVector v1 = new LngBooleanVector();
        assertThat(v1.size()).isEqualTo(0);
        assertThat(v1.isEmpty()).isTrue();
        final LngBooleanVector v2 = new LngBooleanVector(10);
        assertThat(v2.size()).isEqualTo(0);
        assertThat(v2.isEmpty()).isTrue();
        final LngBooleanVector v3 = new LngBooleanVector(10, true);
        assertThat(v3.size()).isEqualTo(10);
        for (int i = 0; i < v3.size(); i++) {
            assertThat(v3.get(i)).isTrue();
        }
        assertThat(v3.isEmpty()).isFalse();
        final LngBooleanVector v4 = new LngBooleanVector(v3);
        assertThat(v4.size()).isEqualTo(10);
        for (int i = 0; i < v4.size(); i++) {
            assertThat(v4.get(i)).isTrue();
        }
        assertThat(v4.isEmpty()).isFalse();
        final LngBooleanVector v5 = new LngBooleanVector(true, true, true, false, false);
        assertThat(v5.size()).isEqualTo(5);
        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                assertThat(v5.get(i)).isTrue();
            } else {
                assertThat(v5.get(i)).isFalse();
            }
        }
    }

    @Test
    public void testVectorAddElements() {
        final LngBooleanVector v1 = new LngBooleanVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i % 2 == 0);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo(i % 2 == 0);
            assertThat(v1.get(i)).isEqualTo(i % 2 == 0);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.clear();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void legalUnsafePush() {
        final LngBooleanVector v1 = new LngBooleanVector(1000);
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.unsafePush(i % 2 == 0);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo(i % 2 == 0);
            assertThat(v1.get(i)).isEqualTo(i % 2 == 0);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.clear();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void illegalUnsafePush() {
        final LngBooleanVector v1 = new LngBooleanVector(100);
        assertThat(v1.isEmpty()).isTrue();
        assertThatThrownBy(() -> {
            for (int i = 0; i < 1000; i++) {
                v1.unsafePush(i % 2 == 0);
            }
        }).isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    public void testGettingSettingAndPopping() {
        final LngBooleanVector v1 = new LngBooleanVector();
        for (int i = 0; i < 1000; i++) {
            v1.push(i % 2 == 0);
        }
        for (int i = 999; i >= 0; i--) {
            v1.set(i, true);
            assertThat(v1.get(i)).isEqualTo(true);
        }
        for (int i = 999; i >= 0; i--) {
            v1.pop();
            assertThat(v1.size()).isEqualTo(i);
        }
    }

    @Test
    public void testVectorShrink() {
        final LngBooleanVector v1 = new LngBooleanVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i % 2 == 0);
        }
        assertThat(v1.isEmpty()).isFalse();
        final int beforeSize = v1.size();
        v1.shrinkTo(v1.size() + 50);
        assertThat(beforeSize).isEqualTo(v1.size());
        for (int i = 500; i > 0; i--) {
            v1.shrinkTo(i);
            assertThat(v1.back()).isEqualTo((i - 1) % 2 == 0);
        }
    }

    @Test
    public void testGrowTo() {
        final LngBooleanVector v1 = new LngBooleanVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i % 2 == 0);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 1001; i += 10) {
            v1.growTo(1000 + i, true);
            assertThat(v1.size()).isEqualTo(1000 + i);
            for (int j = 0; j < 1000; j++) {
                assertThat(v1.get(j)).isEqualTo(j % 2 == 0);
            }
            for (int j = 1000; j < 1000 + i; j++) {
                assertThat(v1.get(j)).isEqualTo(true);
            }
        }
        assertThat(v1.size()).isEqualTo(2000);
        v1.growTo(100, true);
        assertThat(v1.size()).isEqualTo(2000);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(i % 2 == 0);
        }
        for (int i = 1000; i < 2000; i++) {
            assertThat(v1.get(i)).isEqualTo(true);
        }
    }

    @Test
    public void testRemoveElements() {
        final LngBooleanVector v1 = new LngBooleanVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i % 2 == 0);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 9; i++) {
            v1.removeElements(100);
            assertThat(v1.size()).isEqualTo(1000 - (i + 1) * 100);
            assertThat(v1.back()).isEqualTo((1000 - (i + 1) * 100 - 1) % 2 == 0);
        }
        assertThat(v1.size()).isEqualTo(100);
        v1.removeElements(100);
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void testReverseInplace() {
        final LngBooleanVector v1 = new LngBooleanVector(true, true, false, true, false, false, true, true);
        v1.shrinkTo(7);
        v1.reverseInplace();
        assertThat(v1.get(0)).isEqualTo(true);
        assertThat(v1.get(1)).isEqualTo(false);
        assertThat(v1.get(2)).isEqualTo(false);
        assertThat(v1.get(3)).isEqualTo(true);
        assertThat(v1.get(4)).isEqualTo(false);
        assertThat(v1.get(5)).isEqualTo(true);
        assertThat(v1.get(6)).isEqualTo(true);
    }

    @Test
    public void testToArray() {
        final LngBooleanVector v1 = new LngBooleanVector(1000);
        final boolean[] expected = new boolean[500];
        for (int i = 0; i < 1000; i++) {
            v1.push(i % 2 == 0);
            if (i < 500) {
                expected[i] = i % 2 == 0;
            }
        }
        v1.shrinkTo(500);
        assertThat(v1.toArray()).containsExactly(expected);
    }

    @Test
    public void testToString() {
        final LngBooleanVector v1 = new LngBooleanVector();
        assertThat(v1.toString()).isEqualTo("[]");
        v1.push(true);
        assertThat(v1.toString()).isEqualTo("[true]");
        v1.push(false);
        assertThat(v1.toString()).isEqualTo("[true, false]");
        v1.push(false);
        assertThat(v1.toString()).isEqualTo("[true, false, false]");
        v1.push(true);
        assertThat(v1.toString()).isEqualTo("[true, false, false, true]");
    }
}
