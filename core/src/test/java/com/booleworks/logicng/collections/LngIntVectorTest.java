// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

public class LngIntVectorTest {

    @Test
    public void testVectorCreation() {
        final LngIntVector v1 = new LngIntVector();
        assertThat(v1.size()).isEqualTo(0);
        assertThat(v1.isEmpty()).isTrue();
        final LngIntVector v2 = new LngIntVector(10);
        assertThat(v2.size()).isEqualTo(0);
        assertThat(v2.isEmpty()).isTrue();
        final LngIntVector v3 = LngIntVector.of(IntStream.generate(() -> 42).limit(10).toArray());
        assertThat(v3.size()).isEqualTo(10);
        for (int i = 0; i < v3.size(); i++) {
            assertThat(v3.get(i)).isEqualTo(42);
        }
        assertThat(v3.isEmpty()).isFalse();
        final LngIntVector v4 = new LngIntVector(v3);
        assertThat(v4.size()).isEqualTo(10);
        for (int i = 0; i < v4.size(); i++) {
            assertThat(v4.get(i)).isEqualTo(42);
        }
        assertThat(v4.isEmpty()).isFalse();
        final LngIntVector v5 = LngIntVector.of(0, 1, 2, 3, 4);
        assertThat(v5.size()).isEqualTo(5);
        for (int i = 0; i < 5; i++) {
            assertThat(v5.get(i)).isEqualTo(i);
        }
    }

    @Test
    public void testVectorAddElements() {
        final LngIntVector v1 = new LngIntVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo(i);
            assertThat(v1.get(i)).isEqualTo(i);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.clear();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void legalUnsafePush() {
        final LngIntVector v1 = new LngIntVector(1000);
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.unsafePush(i);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo(i);
            assertThat(v1.get(i)).isEqualTo(i);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.clear();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void illegalUnsafePush() {
        final LngIntVector v1 = new LngIntVector(100);
        assertThat(v1.isEmpty()).isTrue();
        assertThatThrownBy(() -> {
            for (int i = 0; i < 1000; i++) {
                v1.unsafePush(i);
            }
        }).isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    public void testGettingSettingAndPopping() {
        final LngIntVector v1 = new LngIntVector();
        for (int i = 0; i < 1000; i++) {
            v1.push(i);
        }
        for (int i = 999; i >= 0; i--) {
            v1.set(i, 42);
            assertThat(v1.get(i)).isEqualTo(42);
        }
        for (int i = 999; i >= 0; i--) {
            v1.pop();
            assertThat(v1.size()).isEqualTo(i);
        }
    }

    @Test
    public void testVectorShrink() {
        final LngIntVector v1 = new LngIntVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 500; i > 0; i--) {
            v1.shrinkTo(i);
            assertThat(v1.back()).isEqualTo((i - 1));
        }
    }

    @Test
    public void testGrowTo() {
        final LngIntVector v1 = new LngIntVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 1001; i += 10) {
            v1.growTo(1000 + i, 1001);
            assertThat(v1.size()).isEqualTo(1000 + i);
            for (int j = 0; j < 1000; j++) {
                assertThat(v1.get(j)).isEqualTo(j);
            }
            for (int j = 1000; j < 1000 + i; j++) {
                assertThat(v1.get(j)).isEqualTo(1001);
            }
        }
        assertThat(v1.size()).isEqualTo(2000);
        v1.growTo(100, 1001);
        assertThat(v1.size()).isEqualTo(2000);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(i);
        }
        for (int i = 1000; i < 2000; i++) {
            assertThat(v1.get(i)).isEqualTo(1001);
        }
    }

    @Test
    public void testRemoveElements() {
        final LngIntVector v1 = new LngIntVector();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push(i);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 9; i++) {
            v1.removeElements(100);
            assertThat(v1.size()).isEqualTo(1000 - (i + 1) * 100);
            assertThat(v1.back()).isEqualTo(1000 - (i + 1) * 100 - 1);
        }
        assertThat(v1.size()).isEqualTo(100);
        v1.removeElements(100);
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void testSort() {
        final LngIntVector v1 = new LngIntVector(1000);
        final LngIntVector v2 = new LngIntVector(1000);
        for (int i = 999; i >= 0; i--) {
            v1.push(i);
        }
        for (int i = 0; i < 1000; i++) {
            v2.push(i);
        }
        v1.sort();
        v2.sort();
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
            if (i != 999) {
                assertThat(v1.get(i) < v1.get(i + 1)).isTrue();
            }
        }
        final LngIntVector v3 = new LngIntVector(1000);
        v3.sort();
        assertThat(v3.isEmpty()).isTrue();
    }

    @Test
    public void testSortReverse() {
        final LngIntVector v1 = new LngIntVector(1000);
        final LngIntVector v2 = new LngIntVector(1000);
        for (int i = 999; i >= 0; i--) {
            v1.push(i);
        }
        for (int i = 0; i < 1000; i++) {
            v2.push(i);
        }
        v1.sortReverse();
        v2.sortReverse();
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
            if (i != 999) {
                assertThat(v1.get(i) > v1.get(i + 1)).isTrue();
            }
        }
        final LngIntVector v3 = new LngIntVector(1000);
        v3.sortReverse();
        assertThat(v3.isEmpty()).isTrue();
    }

    @Test
    public void testToArray() {
        final LngIntVector v1 = new LngIntVector(1000);
        final int[] expected = new int[500];
        for (int i = 0; i < 1000; i++) {
            v1.push(i);
            if (i < 500) {
                expected[i] = i;
            }
        }
        v1.shrinkTo(500);
        assertThat(v1.toArray()).containsExactly(expected);
    }

    @Test
    public void testToString() {
        final LngIntVector v1 = new LngIntVector();
        assertThat(v1.toString()).isEqualTo("[]");
        v1.push(1);
        assertThat(v1.toString()).isEqualTo("[1]");
        v1.push(2);
        assertThat(v1.toString()).isEqualTo("[1, 2]");
        v1.push(3);
        assertThat(v1.toString()).isEqualTo("[1, 2, 3]");
        v1.push(4);
        assertThat(v1.toString()).isEqualTo("[1, 2, 3, 4]");
    }
}
