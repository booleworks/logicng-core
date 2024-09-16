// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.generators;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.datastructures.Hypergraph;
import com.booleworks.logicng.graphs.datastructures.HypergraphEdge;
import com.booleworks.logicng.graphs.datastructures.HypergraphNode;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class HypergraphGeneratorTest {

    @Test
    public void testCNF() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(HypergraphGenerator.fromCnf(f, p.parse("$false")).getNodes()).isEmpty();
        assertThat(HypergraphGenerator.fromCnf(f, p.parse("$false")).getEdges()).isEmpty();
        assertThat(HypergraphGenerator.fromCnf(f, p.parse("$true")).getNodes()).isEmpty();
        assertThat(HypergraphGenerator.fromCnf(f, p.parse("$true")).getEdges()).isEmpty();

        Hypergraph<Variable> hypergraph = HypergraphGenerator.fromCnf(f, p.parse("A"));
        HypergraphNode<Variable> nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        assertThat(hypergraph.getNodes()).containsExactly(nodeA);
        assertThat(hypergraph.getEdges()).containsExactly(new HypergraphEdge<>(Collections.singletonList(nodeA)));

        hypergraph = HypergraphGenerator.fromCnf(f, p.parse("A | B | ~C"));
        nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        HypergraphNode<Variable> nodeB = new HypergraphNode<>(hypergraph, f.variable("B"));
        HypergraphNode<Variable> nodeC = new HypergraphNode<>(hypergraph, f.variable("C"));
        assertThat(hypergraph.getNodes()).containsExactlyInAnyOrder(nodeA, nodeB, nodeC);
        assertThat(hypergraph.getEdges())
                .containsExactlyInAnyOrder(new HypergraphEdge<>(Arrays.asList(nodeA, nodeB, nodeC)));

        hypergraph =
                HypergraphGenerator.fromCnf(f, p.parse("(A | B | ~C) & (B | ~D) & (C | ~E) & (~B | ~D | E) & X & ~Y"));
        nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        nodeB = new HypergraphNode<>(hypergraph, f.variable("B"));
        nodeC = new HypergraphNode<>(hypergraph, f.variable("C"));
        final HypergraphNode<Variable> nodeD = new HypergraphNode<>(hypergraph, f.variable("D"));
        final HypergraphNode<Variable> nodeE = new HypergraphNode<>(hypergraph, f.variable("E"));
        final HypergraphNode<Variable> nodeX = new HypergraphNode<>(hypergraph, f.variable("X"));
        final HypergraphNode<Variable> nodeY = new HypergraphNode<>(hypergraph, f.variable("Y"));
        assertThat(hypergraph.getNodes()).containsExactlyInAnyOrder(nodeA, nodeB, nodeC, nodeD, nodeE, nodeX, nodeY);
        assertThat(hypergraph.getEdges()).containsExactlyInAnyOrder(
                new HypergraphEdge<>(Arrays.asList(nodeA, nodeB, nodeC)),
                new HypergraphEdge<>(Arrays.asList(nodeB, nodeD)),
                new HypergraphEdge<>(Arrays.asList(nodeC, nodeE)),
                new HypergraphEdge<>(Arrays.asList(nodeB, nodeD, nodeE)),
                new HypergraphEdge<>(Collections.singletonList(nodeX)),
                new HypergraphEdge<>(Collections.singletonList(nodeY))
        );
    }

    @Test
    public void testCNFFromList() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("$false"))).getNodes())
                .isEmpty();
        assertThat(HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("$false"))).getEdges())
                .isEmpty();
        assertThat(HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("$true"))).getNodes())
                .isEmpty();
        assertThat(HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("$true"))).getEdges())
                .isEmpty();

        Hypergraph<Variable> hypergraph = HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("A")));
        HypergraphNode<Variable> nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        assertThat(hypergraph.getNodes()).containsExactly(nodeA);
        assertThat(hypergraph.getEdges()).containsExactly(new HypergraphEdge<>(Collections.singletonList(nodeA)));

        hypergraph = HypergraphGenerator.fromCnf(f, Collections.singletonList(p.parse("A | B | ~C")));
        nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        HypergraphNode<Variable> nodeB = new HypergraphNode<>(hypergraph, f.variable("B"));
        HypergraphNode<Variable> nodeC = new HypergraphNode<>(hypergraph, f.variable("C"));
        assertThat(hypergraph.getNodes()).containsExactlyInAnyOrder(nodeA, nodeB, nodeC);
        assertThat(hypergraph.getEdges())
                .containsExactlyInAnyOrder(new HypergraphEdge<>(Arrays.asList(nodeA, nodeB, nodeC)));

        hypergraph = HypergraphGenerator.fromCnf(f, Arrays.asList(
                p.parse("(A | B | ~C)"),
                p.parse("(B | ~D)"),
                p.parse("(C | ~E)"),
                p.parse("(~B | ~D | E)"),
                p.parse("X"),
                p.parse("~Y")
        ));
        nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        nodeB = new HypergraphNode<>(hypergraph, f.variable("B"));
        nodeC = new HypergraphNode<>(hypergraph, f.variable("C"));
        HypergraphNode<Variable> nodeD = new HypergraphNode<>(hypergraph, f.variable("D"));
        HypergraphNode<Variable> nodeE = new HypergraphNode<>(hypergraph, f.variable("E"));
        HypergraphNode<Variable> nodeX = new HypergraphNode<>(hypergraph, f.variable("X"));
        HypergraphNode<Variable> nodeY = new HypergraphNode<>(hypergraph, f.variable("Y"));
        assertThat(hypergraph.getNodes()).containsExactlyInAnyOrder(nodeA, nodeB, nodeC, nodeD, nodeE, nodeX, nodeY);
        assertThat(hypergraph.getEdges()).containsExactlyInAnyOrder(
                new HypergraphEdge<>(Arrays.asList(nodeA, nodeB, nodeC)),
                new HypergraphEdge<>(Arrays.asList(nodeB, nodeD)),
                new HypergraphEdge<>(Arrays.asList(nodeC, nodeE)),
                new HypergraphEdge<>(Arrays.asList(nodeB, nodeD, nodeE)),
                new HypergraphEdge<>(Collections.singletonList(nodeX)),
                new HypergraphEdge<>(Collections.singletonList(nodeY))
        );

        hypergraph = HypergraphGenerator.fromCnf(f,
                p.parse("(A | B | ~C)"),
                p.parse("(B | ~D)"),
                p.parse("(C | ~E)"),
                p.parse("(~B | ~D | E)"),
                p.parse("X"),
                p.parse("~Y")
        );
        nodeA = new HypergraphNode<>(hypergraph, f.variable("A"));
        nodeB = new HypergraphNode<>(hypergraph, f.variable("B"));
        nodeC = new HypergraphNode<>(hypergraph, f.variable("C"));
        nodeD = new HypergraphNode<>(hypergraph, f.variable("D"));
        nodeE = new HypergraphNode<>(hypergraph, f.variable("E"));
        nodeX = new HypergraphNode<>(hypergraph, f.variable("X"));
        nodeY = new HypergraphNode<>(hypergraph, f.variable("Y"));
        assertThat(hypergraph.getNodes()).containsExactlyInAnyOrder(nodeA, nodeB, nodeC, nodeD, nodeE, nodeX, nodeY);
        assertThat(hypergraph.getEdges()).containsExactlyInAnyOrder(
                new HypergraphEdge<>(Arrays.asList(nodeA, nodeB, nodeC)),
                new HypergraphEdge<>(Arrays.asList(nodeB, nodeD)),
                new HypergraphEdge<>(Arrays.asList(nodeC, nodeE)),
                new HypergraphEdge<>(Arrays.asList(nodeB, nodeD, nodeE)),
                new HypergraphEdge<>(Collections.singletonList(nodeX)),
                new HypergraphEdge<>(Collections.singletonList(nodeY))
        );
    }

    @Test
    public void testNonCNF() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        try {
            HypergraphGenerator.fromCnf(f, p.parse("A => B"));
        } catch (final IllegalArgumentException e) {
            assertThat(e).hasMessage("Cannot generate a hypergraph from a non-cnf formula");
        }
    }
}
