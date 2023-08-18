// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.io.graphical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.io.graphical.GraphicalColor.BLACK;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphicalEdgeTest {

    private static final GraphicalDotWriter dotWriter = GraphicalDotWriter.get();
    private static final GraphicalMermaidWriter mermaidWriter = GraphicalMermaidWriter.get();
    private final GraphicalNode n1 = new GraphicalNode("id1", "ID 1", GraphicalNodeStyle.noStyle());
    private final GraphicalNode n2 = new GraphicalNode("id2", "ID 2", GraphicalNodeStyle.noStyle());
    private GraphicalRepresentation grUndirected;
    private GraphicalRepresentation grDirected;

    @BeforeEach
    public void init() {
        grDirected = new GraphicalRepresentation(false, true);
        grUndirected = new GraphicalRepresentation(false, false);
        grDirected.addNode(n1);
        grDirected.addNode(n2);
        grUndirected.addNode(n1);
        grUndirected.addNode(n2);
    }

    @Test
    public void testNoStyle() {
        final GraphicalEdge edge = new GraphicalEdge(n1, n2, GraphicalEdgeStyle.noStyle());
        grDirected.addEdge(edge);
        grUndirected.addEdge(edge);

        assertThat(grDirected.writeString(dotWriter)).contains("id1 -> id2");
        assertThat(grDirected.writeString(mermaidWriter)).contains("id1 --> id2");
        assertThat(grDirected.writeString(mermaidWriter)).doesNotContain("linkStyle");

        assertThat(grUndirected.writeString(dotWriter)).contains("id1 -- id2");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("id1 --- id2");
        assertThat(grUndirected.writeString(mermaidWriter)).doesNotContain("linkStyle");
    }

    @Test
    public void testOnlyEdgeType() {
        final GraphicalEdge edge = new GraphicalEdge(n1, n2, GraphicalEdgeStyle.dotted(null));
        grDirected.addEdge(edge);
        grUndirected.addEdge(edge);

        assertThat(grDirected.writeString(dotWriter)).contains("id1 -> id2 [style=dotted]");
        assertThat(grDirected.writeString(mermaidWriter)).contains("id1 --> id2");
        assertThat(grDirected.writeString(mermaidWriter)).contains("linkStyle 0 stroke-width:2,stroke-dasharray:3");

        assertThat(grUndirected.writeString(dotWriter)).contains("id1 -- id2 [style=dotted]");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("id1 --- id2");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("linkStyle 0 stroke-width:2,stroke-dasharray:3");
    }

    @Test
    public void testOnlyColor() {
        final GraphicalEdge edge = new GraphicalEdge(n1, n2, GraphicalEdgeStyle.style(null, BLACK));
        grDirected.addEdge(edge);
        grUndirected.addEdge(edge);

        assertThat(grDirected.writeString(dotWriter)).contains("id1 -> id2 [color=\"#000000\", fontcolor=\"#000000\"]");
        assertThat(grDirected.writeString(mermaidWriter)).contains("id1 --> id2");
        assertThat(grDirected.writeString(mermaidWriter)).contains("linkStyle 0 stroke:#000000");

        assertThat(grUndirected.writeString(dotWriter)).contains("id1 -- id2 [color=\"#000000\", fontcolor=\"#000000\"]");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("id1 --- id2");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("linkStyle 0 stroke:#000000");
    }

    @Test
    public void testAll() {
        final GraphicalEdge edge = new GraphicalEdge(n1, n2, GraphicalEdgeStyle.bold(BLACK));
        grDirected.addEdge(edge);
        grUndirected.addEdge(edge);

        assertThat(grDirected.writeString(dotWriter)).contains("id1 -> id2 [color=\"#000000\", fontcolor=\"#000000\", style=bold]");
        assertThat(grDirected.writeString(mermaidWriter)).contains("id1 --> id2");
        assertThat(grDirected.writeString(mermaidWriter)).contains("linkStyle 0 stroke:#000000,stroke-width:4");

        assertThat(grUndirected.writeString(dotWriter)).contains("id1 -- id2 [color=\"#000000\", fontcolor=\"#000000\", style=bold]");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("id1 --- id2");
        assertThat(grUndirected.writeString(mermaidWriter)).contains("linkStyle 0 stroke:#000000,stroke-width:4");
    }
}
