// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.GraphTest;
import com.booleworks.logicng.io.graphical.GraphicalColor;
import com.booleworks.logicng.io.graphical.GraphicalDotWriter;
import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.io.graphical.GraphicalMermaidWriter;
import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class GraphGraphicalGeneratorTest {

    @Test
    public void testSmallDefault() throws IOException {
        final Graph<String> g = new Graph<>();
        g.connect(g.node("A"), g.node("B"));
        g.node("C");
        testFiles("small", g, GraphGraphicalGenerator.<String>builder().build());
    }

    @Test
    public void testSmallFixedStyle() throws IOException {
        final Graph<String> g = new Graph<>();
        g.connect(g.node("A"), g.node("B"));
        g.node("C");
        final GraphGraphicalGenerator<String> generator = GraphGraphicalGenerator.<String>builder()
                .backgroundColor(GraphicalColor.hex("#4f4f4f"))
                .defaultNodeStyle(GraphicalNodeStyle.style(null, GraphicalColor.RED, GraphicalColor.GREEN, null))
                .defaultEdgeStyle(GraphicalEdgeStyle.dotted(GraphicalColor.WHITE))
                .build();
        testFiles("small-fixedStyle", g, generator);
    }

    @Test
    public void test30() throws IOException {
        final Graph<Long> g = GraphTest.getLongGraph("30");
        for (long i = 0; i < 30; i++) {
            g.node(i);
        }
        testFiles("30", g, GraphGraphicalGenerator.<Long>builder().build());
    }

    @Test
    public void test30DynamicStyle() throws IOException {
        final Graph<Long> g = GraphTest.getLongGraph("30");
        for (long i = 0; i < 30; i++) {
            g.node(i);
        }
        final GraphicalNodeStyle style1 =
                GraphicalNodeStyle.rectangle(GraphicalColor.GREEN, GraphicalColor.BLACK, GraphicalColor.GREEN);
        final GraphicalNodeStyle style2 =
                GraphicalNodeStyle.ellipse(GraphicalColor.ORANGE, GraphicalColor.BLACK, GraphicalColor.ORANGE);
        final GraphicalNodeStyle style3 =
                GraphicalNodeStyle.circle(GraphicalColor.RED, GraphicalColor.WHITE, GraphicalColor.RED);

        final NodeStyleMapper<Long> mapper = (l) -> {
            if (l <= 10) {
                return style1;
            } else if (l <= 20) {
                return style2;
            } else {
                return style3;
            }
        };

        final GraphicalEdgeStyle eStyle1 = GraphicalEdgeStyle.style(null, GraphicalColor.GREEN);
        final GraphicalEdgeStyle eStyle2 = GraphicalEdgeStyle.solid(GraphicalColor.ORANGE);
        final GraphicalEdgeStyle eStyle3 = GraphicalEdgeStyle.dotted(GraphicalColor.GRAY_LIGHT);

        final EdgeStyleMapper<Long> edgeMapper = (l1, l2) -> {
            if (l1 <= 10 && l2 <= 10) {
                return eStyle1;
            } else if (l1 <= 20 && l2 <= 20) {
                return eStyle2;
            } else {
                return eStyle3;
            }
        };

        final GraphGraphicalGenerator<Long> generator = GraphGraphicalGenerator.<Long>builder()
                .labelMapper((l) -> "value: " + l)
                .nodeStyleMapper(mapper)
                .edgeMapper(edgeMapper)
                .build();
        testFiles("30-dynamic", g, generator);
    }

    @Test
    public void test50p1() throws IOException {
        final Graph<Long> g = GraphTest.getLongGraph("50");
        g.node(51L);
        testFiles("50p1", g, GraphGraphicalGenerator.<Long>builder().build());
    }

    private <T> void testFiles(final String fileName, final Graph<T> g, final GraphGraphicalGenerator<T> generator)
            throws IOException {
        final GraphicalRepresentation representation = generator.translate(g);
        representation.write("../test_files/writers/temp/" + fileName + ".dot", GraphicalDotWriter.get());
        representation.write("../test_files/writers/temp/" + fileName + ".txt", GraphicalMermaidWriter.get());

        final File expectedDot = new File("../test_files/writers/graph/" + fileName + ".dot");
        final File tempDot = new File("../test_files/writers/temp/" + fileName + ".dot");
        assertThat(contentOf(tempDot)).isEqualTo(contentOf(expectedDot));

        final File expectedMermaid = new File("../test_files/writers/graph/" + fileName + ".txt");
        final File tempMermaid = new File("../test_files/writers/temp/" + fileName + ".txt");
        assertThat(contentOf(tempMermaid)).isEqualTo(contentOf(expectedMermaid));
    }
}
