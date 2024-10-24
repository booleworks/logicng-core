// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class LngVectorTest {

    private final Comparator<String> stringComparator;

    public LngVectorTest() {
        stringComparator = String::compareTo;
    }

    @Test
    public void testVectorCreation() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.size()).isEqualTo(0);
        assertThat(v1.isEmpty()).isTrue();
        final LngVector<String> v2 = new LngVector<>(10);
        assertThat(v2.size()).isEqualTo(0);
        assertThat(v2.isEmpty()).isTrue();
        final LngVector<String> v3 = new LngVector<>(10, "string");
        assertThat(v3.size()).isEqualTo(10);
        for (int i = 0; i < v3.size(); i++) {
            assertThat(v3.get(i)).isEqualTo("string");
        }
        assertThat(v3.isEmpty()).isFalse();
        final LngVector<String> v4 = new LngVector<>("s1", "s2", "s3", "s4", "s5");
        assertThat(v4.size()).isEqualTo(5);
        int count = 1;
        for (final String s : v4) {
            assertThat(s).isEqualTo("s" + count++);
        }
        final List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add("s" + i);
        }
        final LngVector<String> v5 = new LngVector<>(list);
        assertThat(v5.size()).isEqualTo(1000);
        for (int i = 0; i < 1000; i++) {
            assertThat(v5.get(i)).isEqualTo("s" + i);
        }
    }

    @Test
    public void testVectorAddElements() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo("s" + i);
            assertThat(v1.get(i)).isEqualTo("s" + i);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.clear();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void testRelease() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo("s" + i);
            assertThat(v1.get(i)).isEqualTo("s" + i);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.release();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void legalUnsafePush() {
        final LngVector<String> v1 = new LngVector<>(1000);
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.unsafePush("s" + i);
            assertThat(v1.size()).isEqualTo(i + 1);
            assertThat(v1.back()).isEqualTo("s" + i);
            assertThat(v1.get(i)).isEqualTo("s" + i);
        }
        assertThat(v1.isEmpty()).isFalse();
        v1.clear();
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void illegalUnsafePush() {
        final LngVector<String> v1 = new LngVector<>(100);
        assertThat(v1.isEmpty()).isTrue();
        assertThatThrownBy(() -> {
            for (int i = 0; i < 1000; i++) {
                v1.unsafePush("s" + i);
            }
        }).isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    public void testGettingSettingAndPopping() {
        final LngVector<String> v1 = new LngVector<>();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        for (int i = 999; i >= 0; i--) {
            v1.set(i, "string");
            assertThat(v1.get(i)).isEqualTo("string");
        }
        for (int i = 999; i >= 0; i--) {
            v1.pop();
            assertThat(v1.size()).isEqualTo(i);
        }
    }

    @Test
    public void testVectorShrink() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        assertThat(v1.isEmpty()).isFalse();
        final int beforeSize = v1.size();
        v1.shrinkTo(v1.size() + 50);
        assertThat(beforeSize).isEqualTo(v1.size());
        for (int i = 500; i > 0; i--) {
            v1.shrinkTo(i);
            assertThat(v1.back()).isEqualTo("s" + (i - 1));
        }
    }

    @Test
    public void testGrowToWithPad() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 1001; i += 10) {
            v1.growTo(1000 + i, "string");
            assertThat(v1.size()).isEqualTo(1000 + i);
            for (int j = 0; j < 1000; j++) {
                assertThat(v1.get(j)).isEqualTo("s" + j);
            }
            for (int j = 1000; j < 1000 + i; j++) {
                assertThat(v1.get(j)).isEqualTo("string");
            }
        }
        assertThat(v1.size()).isEqualTo(2000);
        v1.growTo(100, "string");
        assertThat(v1.size()).isEqualTo(2000);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo("s" + i);
        }
        for (int i = 1000; i < 2000; i++) {
            assertThat(v1.get(i)).isEqualTo("string");
        }
    }

    @Test
    public void testGrowTo() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 2000; i++) {
            v1.push("s" + i);
        }
        v1.shrinkTo(1000);
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 1001; i += 10) {
            v1.growTo(1000 + i);
            assertThat(v1.size()).isEqualTo(1000 + i);
            for (int j = 0; j < 1000; j++) {
                assertThat(v1.get(j)).isEqualTo("s" + j);
            }
            for (int j = 1000; j < 1000 + i; j++) {
                assertThat(v1.get(j)).isEqualTo(null);
            }
        }
        assertThat(v1.size()).isEqualTo(2000);
        v1.growTo(100, "string");
        assertThat(v1.size()).isEqualTo(2000);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo("s" + i);
        }
        for (int i = 1000; i < 2000; i++) {
            assertThat(v1.get(i)).isEqualTo(null);
        }
    }

    @Test
    public void testRemoveElements() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        assertThat(v1.isEmpty()).isFalse();
        for (int i = 0; i < 9; i++) {
            v1.removeElements(100);
            assertThat(v1.size()).isEqualTo(1000 - (i + 1) * 100);
            assertThat(v1.back()).isEqualTo("s" + (1000 - (i + 1) * 100 - 1));
        }
        assertThat(v1.size()).isEqualTo(100);
        v1.removeElements(100);
        assertThat(v1.isEmpty()).isTrue();
    }

    @Test
    public void testInplaceReplace() {
        final LngVector<String> v1 = new LngVector<>();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        final LngVector<String> v2 = new LngVector<>();
        for (int i = 0; i < 500; i++) {
            v2.push("str" + i);
        }
        final LngVector<String> v3 = new LngVector<>();
        for (int i = 0; i < 2000; i++) {
            v3.push("string" + i);
        }
        v1.replaceInplace(v2);
        assertThat(v1.size()).isEqualTo(500);
        for (int i = 0; i < 500; i++) {
            assertThat(v1.get(i)).isEqualTo("str" + i);
        }
        v2.replaceInplace(v3);
        assertThat(v2.size()).isEqualTo(2000);
        for (int i = 0; i < 2000; i++) {
            assertThat(v2.get(i)).isEqualTo("string" + i);
        }
    }

    @Test
    public void testIllegalInplaceReplace() {
        final LngVector<String> v1 = new LngVector<>();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        assertThatThrownBy(() -> v1.replaceInplace(v1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testManualSort() {
        final LngVector<String> v1 = new LngVector<>(1000);
        final LngVector<String> v2 = new LngVector<>(1000);
        for (int i = 999; i >= 0; i--) {
            v1.push("s" + i);
        }
        for (int i = 0; i < 1000; i++) {
            v2.push("s" + i);
        }
        v1.manualSort(stringComparator);
        v2.manualSort(stringComparator);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
            if (i != 999) {
                assertThat(v1.get(i).compareTo(v1.get(i + 1)) < 0).isTrue();
            }
        }
        final LngVector<String> v3 = new LngVector<>(1000);
        v3.manualSort(stringComparator);
        assertThat(v3.isEmpty()).isTrue();
    }

    @Test
    public void testSort() {
        final LngVector<String> v1 = new LngVector<>(1000);
        final LngVector<String> v2 = new LngVector<>(1000);
        for (int i = 999; i >= 0; i--) {
            v1.push("s" + i);
        }
        for (int i = 0; i < 1000; i++) {
            v2.push("s" + i);
        }
        v1.sort(stringComparator);
        v2.sort(stringComparator);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
            if (i != 999) {
                assertThat(v1.get(i).compareTo(v1.get(i + 1)) < 0).isTrue();
            }
        }
        final LngVector<String> v3 = new LngVector<>(1000);
        v3.sort(stringComparator);
        assertThat(v3.isEmpty()).isTrue();
    }

    @Test
    public void testSortReverse() {
        final LngVector<String> v1 = new LngVector<>(1000);
        final LngVector<String> v2 = new LngVector<>(1000);
        for (int i = 999; i >= 0; i--) {
            v1.push("s" + i);
        }
        for (int i = 0; i < 1000; i++) {
            v2.push("s" + i);
        }
        v1.sortReverse(stringComparator);
        v2.sortReverse(stringComparator);
        for (int i = 0; i < 1000; i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
            if (i != 999) {
                assertThat(v1.get(i).compareTo(v1.get(i + 1)) > 0).isTrue();
            }
        }
        final LngVector<String> v3 = new LngVector<>(1000);
        v3.sortReverse(stringComparator);
        assertThat(v3.isEmpty()).isTrue();
    }

    @Test
    public void testRemove() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        v1.remove("s500");
        assertThat(v1.size()).isEqualTo(999);
        assertThat(v1.get(499)).isEqualTo("s499");
        assertThat(v1.get(500)).isEqualTo("s501");
        v1.remove("s0");
        assertThat(v1.size()).isEqualTo(998);
        assertThat(v1.get(0)).isEqualTo("s1");
        assertThat(v1.get(498)).isEqualTo("s499");
        assertThat(v1.get(499)).isEqualTo("s501");
        v1.remove("s999");
        assertThat(v1.size()).isEqualTo(997);
        assertThat(v1.get(0)).isEqualTo("s1");
        assertThat(v1.get(498)).isEqualTo("s499");
        assertThat(v1.get(499)).isEqualTo("s501");
        assertThat(v1.get(996)).isEqualTo("s998");
        v1.remove("s1001");
        assertThat(v1.size()).isEqualTo(997);
        assertThat(v1.get(0)).isEqualTo("s1");
        assertThat(v1.get(498)).isEqualTo("s499");
        assertThat(v1.get(499)).isEqualTo("s501");
        assertThat(v1.get(996)).isEqualTo("s998");
        final LngVector<String> v2 = new LngVector<>("s1", "s1", "s2", "s5", "s8");
        v2.remove("s1");
        assertThat(v2.size()).isEqualTo(4);
        assertThat(v2.toString()).isEqualTo("[s1, s2, s5, s8]");
    }

    @Test
    public void testToArray() {
        final LngVector<String> v1 = new LngVector<>(1000);
        final String[] expected = new String[500];
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
            if (i < 500) {
                expected[i] = "s" + i;
            }
        }
        v1.shrinkTo(500);
        for (int i = 0; i < expected.length; i++) {
            assertThat(v1.get(i)).isEqualTo(expected[i]);
        }
    }

    @Test
    public void testIterator() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        int count = 0;
        for (final String s : v1) {
            assertThat(s).isEqualTo("s" + count++);
        }
    }

    @Test
    public void testIllegalIteratorRemoval() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        final Iterator<String> it = v1.iterator();
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThatThrownBy(it::remove).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testIllegalIteratorTraversal() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.isEmpty()).isTrue();
        for (int i = 0; i < 1000; i++) {
            v1.push("s" + i);
        }
        final Iterator<String> it = v1.iterator();
        assertThat(it.hasNext()).isTrue();
        assertThatThrownBy(() -> {
            for (int i = 0; i < 1001; i++) {
                it.next();
            }
        }).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testToString() {
        final LngVector<String> v1 = new LngVector<>();
        assertThat(v1.toString()).isEqualTo("[]");
        v1.push("s1");
        assertThat(v1.toString()).isEqualTo("[s1]");
        v1.push("s2");
        assertThat(v1.toString()).isEqualTo("[s1, s2]");
        v1.push("s3");
        assertThat(v1.toString()).isEqualTo("[s1, s2, s3]");
        v1.push("s4");
        assertThat(v1.toString()).isEqualTo("[s1, s2, s3, s4]");
    }
}
