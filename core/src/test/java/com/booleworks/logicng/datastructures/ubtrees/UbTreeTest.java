// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.ubtrees;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Unit tests for {@link UbTree}.
 * @version 2.0.0
 * @since 1.5.0
 */
public class UbTreeTest {

    @Test
    public void testEmptySet() {
        final UbTree<String> tree = new UbTree<>();
        tree.addSet(new TreeSet<>());
        assertThat(tree.getRootNodes()).isEmpty();
    }

    @Test
    public void testSingleSet() {
        final UbTree<String> tree = new UbTree<>();
        tree.addSet(set("A", "B", "C"));
        assertThat(tree.getRootNodes()).hasSize(1);
        assertThat(tree.getRootNodes().keySet()).containsExactly("A");
        assertThat(tree.getRootNodes().get("A").getChildren()).hasSize(1);
        assertThat(tree.getRootNodes().get("A").isEndOfPath()).isFalse();
        assertThat(tree.getRootNodes().get("A").getChildren().keySet()).containsExactly("B");
        assertThat(tree.getRootNodes().get("A").getChildren().get("B").getChildren()).hasSize(1);
        assertThat(tree.getRootNodes().get("A").getChildren().get("B").isEndOfPath()).isFalse();
        assertThat(tree.getRootNodes().get("A").getChildren().get("B").getChildren().keySet()).containsExactly("C");
        assertThat(tree.getRootNodes().get("A").getChildren().get("B").getChildren().get("C").isEndOfPath()).isTrue();
        assertThat(tree.getRootNodes().get("A").getChildren().get("B").getChildren().get("C").getSet())
                .isEqualTo(set("A", "B", "C"));
        assertThat(tree.getRootNodes().get("A").getChildren().get("B").getChildren().get("C").getChildren()).isEmpty();
    }

    @Test
    public void testExampleFromPaper() {
        final UbTree<String> tree = new UbTree<>();
        tree.addSet(set("e0", "e1", "e2", "e3"));
        tree.addSet(set("e0", "e1", "e3"));
        tree.addSet(set("e0", "e1", "e2"));
        tree.addSet(set("e2", "e3"));
        assertThat(tree.getRootNodes()).hasSize(2);
        assertThat(tree.getRootNodes().keySet()).containsExactly("e0", "e2");

        // root nodes
        final UbNode<String> e0 = tree.getRootNodes().get("e0");
        final UbNode<String> e2 = tree.getRootNodes().get("e2");
        assertThat(e0.isEndOfPath()).isFalse();
        assertThat(e0.getChildren().keySet()).containsExactly("e1");
        assertThat(e2.isEndOfPath()).isFalse();
        assertThat(e2.getChildren().keySet()).containsExactly("e3");

        // first level
        final UbNode<String> e0e1 = e0.getChildren().get("e1");
        final UbNode<String> e2e3 = e2.getChildren().get("e3");
        assertThat(e0e1.isEndOfPath()).isFalse();
        assertThat(e0e1.getChildren().keySet()).containsExactly("e2", "e3");
        assertThat(e2e3.isEndOfPath()).isTrue();
        assertThat(e2e3.getSet()).isEqualTo(set("e2", "e3"));
        assertThat(e2e3.getChildren().keySet()).isEmpty();

        // second level
        final UbNode<String> e0e1e2 = e0e1.getChildren().get("e2");
        assertThat(e0e1e2.isEndOfPath()).isTrue();
        assertThat(e0e1e2.getSet()).isEqualTo(set("e0", "e1", "e2"));
        assertThat(e0e1e2.getChildren().keySet()).containsExactly("e3");
        final UbNode<String> e0e1e3 = e0e1.getChildren().get("e3");
        assertThat(e0e1e3.isEndOfPath()).isTrue();
        assertThat(e0e1e3.getSet()).isEqualTo(set("e0", "e1", "e3"));
        assertThat(e0e1e3.getChildren().keySet()).isEmpty();

        // third level
        final UbNode<String> e0e1e2e3 = e0e1e2.getChildren().get("e3");
        assertThat(e0e1e2e3.isEndOfPath()).isTrue();
        assertThat(e0e1e2e3.getSet()).isEqualTo(set("e0", "e1", "e2", "e3"));
        assertThat(e0e1e2e3.getChildren().keySet()).isEmpty();
    }

    @Test
    public void testContainsSubset() {
        final UbTree<String> tree = new UbTree<>();
        final SortedSet<String> e0123 = set("e0", "e1", "e2", "e3");
        final SortedSet<String> e013 = set("e0", "e1", "e3");
        final SortedSet<String> e012 = set("e0", "e1", "e2");
        final SortedSet<String> e23 = set("e2", "e3");
        tree.addSet(e0123);
        tree.addSet(e013);
        tree.addSet(e012);
        tree.addSet(e23);
        assertThat(tree.firstSubset(set("e0"))).isNull();
        assertThat(tree.firstSubset(set("e1"))).isNull();
        assertThat(tree.firstSubset(set("e2"))).isNull();
        assertThat(tree.firstSubset(set("e3"))).isNull();
        assertThat(tree.firstSubset(set("e0", "e1"))).isNull();
        assertThat(tree.firstSubset(set("e0", "e2"))).isNull();
        assertThat(tree.firstSubset(set("e0", "e3"))).isNull();
        assertThat(tree.firstSubset(set("e1", "e2"))).isNull();
        assertThat(tree.firstSubset(set("e1", "e3"))).isNull();
        assertThat(tree.firstSubset(set("e2", "e3"))).isEqualTo(e23);
        assertThat(tree.firstSubset(set("e0", "e1", "e2"))).isEqualTo(e012);
        assertThat(tree.firstSubset(set("e0", "e1", "e3"))).isEqualTo(e013);
        assertThat(tree.firstSubset(set("e0", "e2", "e3"))).isEqualTo(e23);
        assertThat(tree.firstSubset(set("e1", "e2", "e3"))).isEqualTo(e23);
        assertThat(tree.firstSubset(set("e0", "e1", "e2", "e3"))).isIn(e0123, e013, e012, e23);
        assertThat(tree.firstSubset(set("e0", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e1", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e2", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e3", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e0", "e1", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e0", "e2", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e0", "e3", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e1", "e2", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e1", "e3", "e4"))).isNull();
        assertThat(tree.firstSubset(set("e2", "e3", "e4"))).isEqualTo(e23);
        assertThat(tree.firstSubset(set("e0", "e1", "e2", "e4"))).isEqualTo(e012);
        assertThat(tree.firstSubset(set("e0", "e1", "e3", "e4"))).isEqualTo(e013);
        assertThat(tree.firstSubset(set("e0", "e2", "e3", "e4"))).isEqualTo(e23);
        assertThat(tree.firstSubset(set("e1", "e2", "e3", "e4"))).isEqualTo(e23);
        assertThat(tree.firstSubset(set("e0", "e1", "e2", "e3", "e4"))).isIn(e0123, e013, e012, e23);
    }

    @Test
    public void testAllSubsets() {
        final UbTree<String> tree = new UbTree<>();
        final SortedSet<String> e0123 = set("e0", "e1", "e2", "e3");
        final SortedSet<String> e013 = set("e0", "e1", "e3");
        final SortedSet<String> e012 = set("e0", "e1", "e2");
        final SortedSet<String> e23 = set("e2", "e3");
        tree.addSet(e0123);
        tree.addSet(e013);
        tree.addSet(e012);
        tree.addSet(e23);
        assertThat(tree.allSubsets(set("e0"))).isEmpty();
        assertThat(tree.allSubsets(set("e1"))).isEmpty();
        assertThat(tree.allSubsets(set("e2"))).isEmpty();
        assertThat(tree.allSubsets(set("e3"))).isEmpty();
        assertThat(tree.allSubsets(set("e0", "e1"))).isEmpty();
        assertThat(tree.allSubsets(set("e0", "e2"))).isEmpty();
        assertThat(tree.allSubsets(set("e0", "e3"))).isEmpty();
        assertThat(tree.allSubsets(set("e1", "e2"))).isEmpty();
        assertThat(tree.allSubsets(set("e1", "e3"))).isEmpty();
        assertThat(tree.allSubsets(set("e2", "e3"))).containsExactlyInAnyOrder(e23);
        assertThat(tree.allSubsets(set("e0", "e1", "e2"))).containsExactlyInAnyOrder(e012);
        assertThat(tree.allSubsets(set("e0", "e1", "e3"))).containsExactlyInAnyOrder(e013);
        assertThat(tree.allSubsets(set("e0", "e2", "e3"))).containsExactlyInAnyOrder(e23);
        assertThat(tree.allSubsets(set("e1", "e2", "e3"))).containsExactlyInAnyOrder(e23);
        assertThat(tree.allSubsets(set("e0", "e1", "e2", "e3"))).containsExactlyInAnyOrder(e0123, e013, e012, e23);
        assertThat(tree.allSubsets(set("e0", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e1", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e2", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e3", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e0", "e1", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e0", "e2", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e0", "e3", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e1", "e2", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e1", "e3", "e4"))).isEmpty();
        assertThat(tree.allSubsets(set("e2", "e3", "e4"))).containsExactlyInAnyOrder(e23);
        assertThat(tree.allSubsets(set("e0", "e1", "e2", "e4"))).containsExactlyInAnyOrder(e012);
        assertThat(tree.allSubsets(set("e0", "e1", "e3", "e4"))).containsExactlyInAnyOrder(e013);
        assertThat(tree.allSubsets(set("e0", "e2", "e3", "e4"))).containsExactlyInAnyOrder(e23);
        assertThat(tree.allSubsets(set("e1", "e2", "e3", "e4"))).containsExactlyInAnyOrder(e23);
        assertThat(tree.allSubsets(set("e0", "e1", "e2", "e3", "e4"))).containsExactlyInAnyOrder(e0123, e013, e012,
                e23);
    }

    @Test
    public void testAllSupersets() {
        final UbTree<String> tree = new UbTree<>();
        final SortedSet<String> e0123 = set("e0", "e1", "e2", "e3");
        final SortedSet<String> e013 = set("e0", "e1", "e3");
        final SortedSet<String> e012 = set("e0", "e1", "e2");
        final SortedSet<String> e23 = set("e2", "e3");
        tree.addSet(e0123);
        tree.addSet(e013);
        tree.addSet(e012);
        tree.addSet(e23);
        assertThat(tree.allSupersets(set("e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0"))).containsExactlyInAnyOrder(e0123, e012, e013);
        assertThat(tree.allSupersets(set("e1"))).containsExactlyInAnyOrder(e0123, e012, e013);
        assertThat(tree.allSupersets(set("e2"))).containsExactlyInAnyOrder(e0123, e012, e23);
        assertThat(tree.allSupersets(set("e3"))).containsExactlyInAnyOrder(e0123, e013, e23);
        assertThat(tree.allSupersets(set("e0", "e1"))).containsExactlyInAnyOrder(e0123, e012, e013);
        assertThat(tree.allSupersets(set("e0", "e2"))).containsExactlyInAnyOrder(e0123, e012);
        assertThat(tree.allSupersets(set("e0", "e3"))).containsExactlyInAnyOrder(e0123, e013);
        assertThat(tree.allSupersets(set("e1", "e2"))).containsExactlyInAnyOrder(e0123, e012);
        assertThat(tree.allSupersets(set("e1", "e3"))).containsExactlyInAnyOrder(e0123, e013);
        assertThat(tree.allSupersets(set("e2", "e3"))).containsExactlyInAnyOrder(e0123, e23);
        assertThat(tree.allSupersets(set("e0", "e1", "e2"))).containsExactlyInAnyOrder(e0123, e012);
        assertThat(tree.allSupersets(set("e0", "e2", "e3"))).containsExactlyInAnyOrder(e0123);
        assertThat(tree.allSupersets(set("e0", "e1", "e2", "e3"))).containsExactlyInAnyOrder(e0123);
        assertThat(tree.allSupersets(set("e0", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e1", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e2", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e1", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e2", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e1", "e2", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e1", "e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e2", "e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e1", "e2", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e1", "e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e2", "e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e1", "e2", "e3", "e4"))).isEmpty();
        assertThat(tree.allSupersets(set("e0", "e1", "e2", "e3", "e4"))).isEmpty();
    }

    @Test
    public void testAllSets() {
        final UbTree<String> tree = new UbTree<>();
        final SortedSet<String> e0123 = set("e0", "e1", "e2", "e3");
        final SortedSet<String> e013 = set("e0", "e1", "e3");
        final SortedSet<String> e012 = set("e0", "e1", "e2");
        final SortedSet<String> e23 = set("e2", "e3");
        tree.addSet(e0123);
        assertThat(tree.allSets()).containsExactlyInAnyOrder(e0123);
        tree.addSet(e013);
        assertThat(tree.allSets()).containsExactlyInAnyOrder(e0123, e013);
        tree.addSet(e012);
        assertThat(tree.allSets()).containsExactlyInAnyOrder(e0123, e013, e012);
        tree.addSet(e23);
        assertThat(tree.allSets()).containsExactlyInAnyOrder(e0123, e013, e012, e23);
    }

    private SortedSet<String> set(final String... elements) {
        return new TreeSet<>(Arrays.asList(elements));
    }
}
