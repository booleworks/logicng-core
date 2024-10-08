// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollectionHelperTest {

    @Test
    public void testNullOrEmpty() {
        assertThat(CollectionHelper.nullOrEmpty(null)).isTrue();
        assertThat(CollectionHelper.nullOrEmpty(Collections.emptySet())).isTrue();
        assertThat(CollectionHelper.nullOrEmpty(Collections.emptyMap().entrySet())).isTrue();
        assertThat(CollectionHelper.nullOrEmpty(Collections.singletonList(2))).isFalse();
    }

    @Test
    public void testNullSafe() {
        assertThat(CollectionHelper.nullSafe(null)).isEmpty();
        final List<String> strings = Arrays.asList("a", "b", "c");
        assertThat(CollectionHelper.nullSafe(strings)).isEqualTo(strings);

        assertThat(CollectionHelper.nullSafe(() -> Arrays.asList("a", "b"), ArrayList::new))
                .isEqualTo(new ArrayList<>(Arrays.asList("a", "b")));
        assertThat(CollectionHelper.nullSafe(() -> new TreeSet<>(Arrays.asList("a", "b")), TreeSet::new))
                .isEqualTo(new TreeSet<>(Arrays.asList("a", "b")));
        assertThat(CollectionHelper.nullSafe(() -> (ArrayList<Integer>) null, ArrayList::new))
                .isEqualTo(new ArrayList<Integer>());
        assertThat(CollectionHelper.nullSafe(() -> (TreeSet<String>) null, TreeSet::new))
                .isEqualTo(new TreeSet<String>());
    }

    @Test
    public void testIntersection() {
        final List<String> strings1 = Arrays.asList("a", "b", "c", "a");
        final List<String> strings2 = Arrays.asList("c", "d", "e");
        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(strings1, null, TreeSet::new)).isEmpty();
        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(null, strings2, TreeSet::new)).isEmpty();
        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(strings1, strings2, TreeSet::new))
                .containsExactly("c");
        final List<String> strings3 = Arrays.asList("c", "d", "e", "a", "a", "a");
        assertThat(CollectionHelper.<String, List<String>>intersection(strings1, strings3, ArrayList::new))
                .containsExactly("a", "c", "a");

        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(
                Arrays.asList(null, strings1, null, strings2, strings1), TreeSet::new)).isEmpty();
        assertThat(CollectionHelper.<String, ArrayList<String>>intersection(
                Arrays.asList(strings1, null, strings3, strings1), ArrayList::new)).isEmpty();
        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(Arrays.asList(strings3, strings2, strings3),
                TreeSet::new))
                        .containsExactly("c", "d", "e");
        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(Arrays.asList(strings1, strings2, strings3),
                TreeSet::new))
                        .containsExactly("c");
        assertThat(CollectionHelper.<String, ArrayList<String>>intersection(Arrays.asList(strings1, strings3),
                ArrayList::new))
                        .containsExactly("a", "c", "a");

        final List<List<String>> lists = Arrays.asList(strings1, strings2);
        assertThat(CollectionHelper.<String, TreeSet<String>>intersection(lists, TreeSet::new)).containsExactly("c");
    }

    @Test
    public void testUnion() {
        final List<String> strings1 = Arrays.asList("a", "b", "c", "a");
        final List<String> strings2 = Arrays.asList("c", "d", "e");
        assertThat(CollectionHelper.<String, ArrayList<String>>union(strings1, null, ArrayList::new))
                .containsExactly("a", "b", "c", "a");
        assertThat(CollectionHelper.<String, ArrayList<String>>union(null, strings2, ArrayList::new))
                .containsExactly("c", "d", "e");
        assertThat(CollectionHelper.<String, ArrayList<String>>union(strings1, strings2, ArrayList::new))
                .containsExactly("a", "b", "c", "a", "c", "d", "e");

        final List<String> strings3 = Arrays.asList("c", "d", "e", "a", "a", "a", "f");
        assertThat(CollectionHelper.<String, TreeSet<String>>union(Arrays.asList(null, strings1, null), TreeSet::new))
                .containsExactly("a", "b", "c");
        assertThat(CollectionHelper.<String, TreeSet<String>>union(
                Arrays.asList(null, strings1, strings2, null, strings3, strings3), TreeSet::new))
                        .containsExactly("a", "b", "c", "d", "e", "f");
        assertThat(CollectionHelper.<String, ArrayList<String>>union(
                Arrays.asList(null, strings3, strings2, null, strings1), ArrayList::new))
                        .containsExactly("c", "d", "e", "a", "a", "a", "f", "c", "d", "e", "a", "b", "c", "a");

        final List<List<String>> lists = Arrays.asList(strings1, strings2);
        assertThat(CollectionHelper.<String, TreeSet<String>>union(lists, TreeSet::new)).containsExactly("a", "b", "c",
                "d", "e");
    }

    @Test
    public void testDifference() {
        final List<String> strings1 = Arrays.asList("a", "b", "c", "a");
        final List<String> strings2 = Arrays.asList("c", "d", "e");
        assertThat(CollectionHelper.<String, TreeSet<String>>difference(strings1, null, TreeSet::new))
                .containsExactly("a", "b", "c");
        assertThat(CollectionHelper.<String, TreeSet<String>>difference(null, strings1, TreeSet::new)).isEmpty();
        assertThat(CollectionHelper.<String, TreeSet<String>>difference(strings1, strings2, TreeSet::new))
                .containsExactly("a", "b");
        assertThat(CollectionHelper.<String, ArrayList<String>>difference(strings1, strings2, ArrayList::new))
                .containsExactly("a", "b", "a");
    }

    @Test
    public void testMkString() {
        assertThat(CollectionHelper.mkString(Collections.emptyList(), ",")).isEqualTo("");
        assertThat(CollectionHelper.mkString(Collections.emptyList(), ",")).isEqualTo("");
        assertThat(CollectionHelper.mkString(Collections.emptySet(), "-")).isEqualTo("");
        assertThat(CollectionHelper.mkString(Collections.emptyMap().entrySet(), "!")).isEqualTo("");
        assertThat(CollectionHelper.mkString(Collections.emptyMap().entrySet(), "[", ",", "]")).isEqualTo("[]");
        assertThat(CollectionHelper.mkString(new Integer[0], "!")).isEqualTo("");
        assertThat(CollectionHelper.mkString(new Integer[0], "(", "!", ")")).isEqualTo("()");

        final List<String> collection01 = Arrays.asList("a", "b", "c");
        assertThat(CollectionHelper.mkString(collection01, ",")).isEqualTo("a,b,c");
        assertThat(CollectionHelper.mkString(collection01, ", ")).isEqualTo("a, b, c");
        assertThat(CollectionHelper.mkString(collection01, "")).isEqualTo("abc");
        assertThat(CollectionHelper.mkString(collection01, "---")).isEqualTo("a---b---c");
        assertThat(CollectionHelper.mkString(collection01, "[", "---", "]")).isEqualTo("[a---b---c]");

        final List<Integer> collection02 = Arrays.asList(1, 2, 3);
        assertThat(CollectionHelper.mkString(collection02, ",")).isEqualTo("1,2,3");
        assertThat(CollectionHelper.mkString(collection02, ", ")).isEqualTo("1, 2, 3");
        assertThat(CollectionHelper.mkString(collection02, "")).isEqualTo("123");
        assertThat(CollectionHelper.mkString(collection02, "---")).isEqualTo("1---2---3");
        assertThat(CollectionHelper.mkString(collection02, "[", "---", "]")).isEqualTo("[1---2---3]");

        final SortedSet<Character> collection03 = new TreeSet<>(Arrays.asList('x', 'y'));
        assertThat(CollectionHelper.mkString(collection03, ",")).isEqualTo("x,y");
        assertThat(CollectionHelper.mkString(collection03, ", ")).isEqualTo("x, y");
        assertThat(CollectionHelper.mkString(collection03, "")).isEqualTo("xy");
        assertThat(CollectionHelper.mkString(collection03, "---")).isEqualTo("x---y");
        assertThat(CollectionHelper.mkString(collection03, "[", "---", "]")).isEqualTo("[x---y]");

        final String[] array01 = new String[]{"x", "y"};
        assertThat(CollectionHelper.mkString(array01, ",")).isEqualTo("x,y");
        assertThat(CollectionHelper.mkString(array01, ", ")).isEqualTo("x, y");
        assertThat(CollectionHelper.mkString(array01, "")).isEqualTo("xy");
        assertThat(CollectionHelper.mkString(array01, "---")).isEqualTo("x---y");
        assertThat(CollectionHelper.mkString(array01, "[", "---", "]")).isEqualTo("[x---y]");

        final List<Integer> array02 = Arrays.asList(1, 2, 3);
        assertThat(CollectionHelper.mkString(array02, ",")).isEqualTo("1,2,3");
        assertThat(CollectionHelper.mkString(array02, ", ")).isEqualTo("1, 2, 3");
        assertThat(CollectionHelper.mkString(array02, "")).isEqualTo("123");
        assertThat(CollectionHelper.mkString(array02, "---")).isEqualTo("1---2---3");
        assertThat(CollectionHelper.mkString(array02, "[", "---", "]")).isEqualTo("[1---2---3]");
    }
}
