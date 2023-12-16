// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.ubtrees;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class UBNodeTest {

    private final UBNode<Integer> node1;
    private final UBNode<String> node2;

    public UBNodeTest() {
        node1 = new UBNode<>(1);
        node2 = new UBNode<>("String");
    }

    @Test
    public void testHashCode() {
        assertThat(node1.hashCode()).isEqualTo(node1.hashCode());
        assertThat(node1.hashCode()).isEqualTo(new UBNode<>(1).hashCode());
    }

    @Test
    public void testEquals() {
        final FormulaFactory f = FormulaFactory.nonCaching();
        assertThat(node1.hashCode()).isEqualTo(node1.hashCode());
        final List<SortedSet<Literal>> primeImplicants = new ArrayList<>();
        primeImplicants.add(new TreeSet<>(List.of(f.variable("a"), f.literal("b", false))));
        primeImplicants.add(new TreeSet<>(List.of(f.variable("a"), f.variable("c"))));
        final List<SortedSet<Literal>> primeImplicates = new ArrayList<>();
        primeImplicates.add(new TreeSet<>(List.of(f.variable("a"), f.literal("b", false))));
        assertThat(node1.equals(node1)).isTrue();
        assertThat(node1.equals(new UBNode<>(1))).isTrue();
        assertThat(node1.equals(node2)).isFalse();
        assertThat(node2.equals(node1)).isFalse();
        assertThat(node1.equals(null)).isFalse();
    }

    @Test
    public void testToString() {
        assertThat(node1.toString()).isEqualTo(
                "UBNode{" +
                        "element=1" +
                        ", children={}" +
                        ", set=null" +
                        '}');
    }
}
