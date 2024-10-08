// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.datastructures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.generators.HypergraphGenerator;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HypergraphEdgeTest {

    private final Offset<Double> offset = Offset.offset(0.000001);

    @Test
    public void testSimpleMethods() {
        final Hypergraph<String> hypergraph = new Hypergraph<>();
        final HypergraphNode<String> node1 = new HypergraphNode<>(hypergraph, "A");
        final HypergraphNode<String> node2 = new HypergraphNode<>(hypergraph, "B");
        final HypergraphEdge<String> edge1 = new HypergraphEdge<>(node1, node2);
        assertThat(edge1.getNodes()).containsExactlyInAnyOrder(node1, node2);
    }

    @Test
    public void testCenterOfGravity() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Hypergraph<Variable> hypergraph =
                HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("A | B | ~C | D")));
        final HypergraphEdge<Variable> edge = hypergraph.getEdges().iterator().next();
        final Map<HypergraphNode<Variable>, Integer> ordering = new HashMap<>();
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("A")), 1);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("B")), 2);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("C")), 3);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("D")), 4);
        assertThat(edge.centerOfGravity(ordering)).isCloseTo(2.5, offset);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("A")), 2);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("B")), 4);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("C")), 6);
        ordering.put(new HypergraphNode<>(hypergraph, f.variable("D")), 8);
        assertThat(edge.centerOfGravity(ordering)).isCloseTo(5, offset);
    }

    @Test
    public void testIllegalCenterOfGravity() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final PropositionalParser p = new PropositionalParser(f);
        final Hypergraph<Variable> hypergraph =
                HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("A | B | ~C | D")));
        final HypergraphEdge<Variable> edge = hypergraph.getEdges().iterator().next();
        assertThatThrownBy(() -> edge.centerOfGravity(new HashMap<>())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testEquals() {
        final Hypergraph<String> hypergraph = new Hypergraph<>();
        final HypergraphNode<String> node1 = new HypergraphNode<>(hypergraph, "A");
        final HypergraphNode<String> node2 = new HypergraphNode<>(hypergraph, "B");
        final HypergraphNode<String> node3 = new HypergraphNode<>(hypergraph, "C");
        final HypergraphEdge<String> edge1 = new HypergraphEdge<>(node1, node2);
        final HypergraphEdge<String> edge2 = new HypergraphEdge<>(node2, node3);
        final HypergraphEdge<String> edge3 = new HypergraphEdge<>(node1, node2);

        assertThat(edge1.equals(null)).isFalse();
        assertThat(edge1).isNotEqualTo(42);

        assertThat(edge1).isEqualTo(edge1);
        assertThat(edge1.equals(edge1)).isTrue();
        assertThat(edge1).isEqualTo(edge3);
        assertThat(edge3).isEqualTo(edge1);
        assertThat(edge1).isNotEqualTo(edge2);
        assertThat(edge2).isNotEqualTo(edge1);
    }
}
