// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.datastructures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GraphTest {

    public static Graph<Long> getLongGraph(final String id) throws IOException {
        final Graph<Long> g = new Graph<>(id + "-Long");

        final BufferedReader reader =
                new BufferedReader(new FileReader("src/test/resources/graphs/graph" + id + ".txt"));

        while (reader.ready()) {
            final String line = reader.readLine().trim();
            final String[] nodePair = line.split(":");
            g.connect(g.node(Long.valueOf(nodePair[0])), g.node(Long.valueOf(nodePair[1])));
        }

        return g;
    }

    @Test
    public void testLongGraph() {
        final Graph<Long> g = new Graph<>("Graph with Long nodes.");

        final Node<Long> a = g.node(1L);
        final Node<Long> b = g.node(2L);
        final Node<Long> c = g.node(3L);
        final Node<Long> d = g.node(4L);
        final Node<Long> d2 = g.node(4L);

        g.connect(d, d2);
        assertThat(d.neighbours().contains(d2)).isFalse();

        g.connect(a, b);
        g.connect(a, c);
        g.connect(a, d);
        g.connect(b, c);

        d.connectTo(d2);
        assertThat(d2.neighbours().contains(d)).isFalse();

        assertThat(a.neighbours().size()).isEqualTo(3);
        assertThat(c.neighbours().contains(a)).isTrue();
        assertThat(c.neighbours().contains(b)).isTrue();

        assertThat(a.toString()).isEqualTo("Node{content=1, neighbours:2,3,4}");
    }

    @Test
    public void testFormulaGraph() {
        final FormulaFactory f = FormulaFactory.caching();
        final Graph<Formula> g = new Graph<>("Graph with Formula nodes.");

        final Variable a = f.variable("A");
        final Node<Formula> an = g.node(a);
        final Variable b = f.variable("B");
        final Node<Formula> bn = g.node(b);

        final Formula aNb = f.and(a, b);
        final Node<Formula> aNbn = g.node(aNb);
        g.connect(aNbn, an);
        g.connect(aNbn, bn);

        assertThat(aNbn.neighbours().size()).isEqualTo(2);
        assertThat(an.neighbours().contains(aNbn)).isTrue();
        assertThat(bn.neighbours().contains(aNbn)).isTrue();

        assertThat(an.graph().name()).isEqualTo(g.name());

        g.disconnect(aNbn, an);

        assertThat(an.neighbours().isEmpty()).isTrue();

        assertThat(g.nodes().size()).isEqualTo(3);
    }

    @Test
    public void testTwoGraphs() {
        final Graph<String> g1 = new Graph<>("G1");
        final Graph<String> g2 = new Graph<>("G2");

        final Node<String> a = g1.node("A");
        final Node<String> b = g2.node("B");

        g1.disconnect(a, b);
        assertThat(a.neighbours().isEmpty()).isTrue();
        assertThatThrownBy(() -> g1.connect(a, b)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNodes() {
        final Graph<String> g1 = new Graph<>("G1");
        final Node<String> node01 = new Node<>("nA", g1);
        final Node<String> node02 = new Node<>("nA", g1);
        final Node<String> node03 = new Node<>("nB", g1);

        assertThat(node01).isEqualTo(node01);
        assertThat(node01.equals(node01)).isTrue();
        assertThat(node01).isEqualTo(node02);
        assertThat(node02).isEqualTo(node01);
        assertThat(node01.equals(2)).isFalse();
        assertThat(node02).isNotEqualTo(node03);
        assertThat(node03).isNotEqualTo(node02);
    }
}
