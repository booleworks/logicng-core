// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.algorithms;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.GraphTest;
import com.booleworks.logicng.graphs.datastructures.Node;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class BronKerboschTest {

    @Test
    public void graph50Test() throws IOException {
        final Graph<Long> g = GraphTest.getLongGraph("50");

        final BronKerbosch<Long> bkp = new BronKerbosch<>(g);
        final Set<SortedSet<Node<Long>>> resultBkp = bkp.compute();

        assertThat(resultBkp.size()).isEqualTo(910);

        for (final SortedSet<Node<Long>> clique1 : resultBkp) {
            for (final SortedSet<Node<Long>> clique2 : resultBkp) {
                if (clique1.size() != clique2.size()) {
                    assertThat(clique1.containsAll(clique2)).isFalse();
                }
            }
        }

        final Node<Long> eleven = g.node(11L);
        for (final Node<Long> nb : eleven.getNeighbours()) {
            g.disconnect(nb, eleven);
        }

        final Set<SortedSet<Node<Long>>> resultEleven = bkp.compute();
        int elevenCliques = 0;
        for (final SortedSet<Node<Long>> clique : resultEleven) {
            if (clique.contains(eleven)) {
                elevenCliques++;
                assertThat(clique.size()).isEqualTo(1);
            }
        }
        assertThat(elevenCliques).isEqualTo(1);

        g.connect(eleven, g.node(10L));

        bkp.compute();
        int tenCliques = 0;
        for (final List<Long> clique : bkp.getCliquesAsTLists()) {
            if (clique.contains(11L)) {
                tenCliques++;
                assertThat(clique.size()).isEqualTo(2);
                assertThat(clique.contains(10L)).isTrue();
            }
        }
        assertThat(tenCliques).isEqualTo(1);
    }
}
