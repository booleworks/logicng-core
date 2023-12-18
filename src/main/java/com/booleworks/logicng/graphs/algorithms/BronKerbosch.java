// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.algorithms;

import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class implements the Bron-Kerbosch-Algorithm, used to compute all
 * maximal Cliques of a Graph. Requires that the ids of the nodes are
 * comparable.
 * @param <T> the node type of the graph
 * @version 2.0.0
 * @since 1.2
 */
public final class BronKerbosch<T extends Comparable<T>> {

    private final Graph<T> g;
    private final Comparator<Node<T>> nodeComparator;
    private final Set<SortedSet<Node<T>>> cliques;

    /**
     * Constructor.
     * @param g the graph whose maximal cliques are to be computed
     */
    public BronKerbosch(final Graph<T> g) {
        this.g = g;
        nodeComparator = Comparator.comparing(Node::content);
        cliques = new LinkedHashSet<>();
    }

    /**
     * Computes the maximal cliques and returns them as a Set of SortedSets of
     * Nodes.
     * @return the maximal cliques.
     */
    public Set<SortedSet<Node<T>>> compute() {
        cliques.clear();
        final SortedSet<Node<T>> p = new TreeSet<>(nodeComparator);
        p.addAll(g.nodes());
        bk(new TreeSet<>(nodeComparator), p, new TreeSet<>(nodeComparator));
        return cliques;
    }

    private void bk(final SortedSet<Node<T>> r, final SortedSet<Node<T>> p, final SortedSet<Node<T>> x) {
        if (p.isEmpty() && x.isEmpty()) {
            cliques.add(r);
            return;
        }
        final SortedSet<Node<T>> pvx = new TreeSet<>(new NodeNeighbourComparator());
        pvx.addAll(p);
        pvx.addAll(x);
        final Node<T> u = pvx.last();
        final SortedSet<Node<T>> pwnu = new TreeSet<>(nodeComparator);
        pwnu.addAll(p);
        pwnu.removeAll(u.neighbours());
        for (final Node<T> v : pwnu) {
            final SortedSet<Node<T>> nr = new TreeSet<>(nodeComparator);
            nr.addAll(r);
            nr.add(v);
            final SortedSet<Node<T>> np = new TreeSet<>(nodeComparator);
            final SortedSet<Node<T>> nx = new TreeSet<>(nodeComparator);
            for (final Node<T> neigh : v.neighbours()) {
                if (p.contains(neigh)) {
                    np.add(neigh);
                }
                if (x.contains(neigh)) {
                    nx.add(neigh);
                }
            }
            bk(nr, np, nx);
            p.remove(v);
            x.add(v);
        }
    }

    /**
     * Returns the maximal cliques computed with the last call to compute() as a
     * List of Lists of T.
     * @return the maximal cliques
     */
    public List<List<T>> getCliquesAsTLists() {
        final List<List<T>> result = new ArrayList<>();
        for (final Set<Node<T>> clique : cliques) {
            final List<T> curList = new ArrayList<>();
            for (final Node<T> node : clique) {
                curList.add(node.content());
            }
            result.add(curList);
        }
        return result;
    }

    /**
     * A comparator between nodes, that compares them by number of neighbours.
     * @version 1.2
     * @since 1.2
     */
    private class NodeNeighbourComparator implements Comparator<Node<T>> {

        @Override
        public int compare(final Node<T> n1, final Node<T> n2) {
            if (n1.neighbours().size() > n2.neighbours().size()) {
                return 1;
            } else if (n1.neighbours().size() < n2.neighbours().size()) {
                return -1;
            } else {
                return nodeComparator.compare(n1, n2);
            }
        }
    }
}
