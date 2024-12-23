// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.datastructures;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A node in a hypergraph.
 * @param <T> the content type of the graph's nodes
 * @version 3.0.0
 * @since 1.4.0
 */
public final class HypergraphNode<T> {

    private final Hypergraph<T> graph;
    private final T content;
    private final LinkedHashSet<HypergraphEdge<T>> edges;

    /**
     * Constructs a new hypergraph node for a given graph and content.
     * @param graph   the graph for this node
     * @param content the content of this node
     */
    public HypergraphNode(final Hypergraph<T> graph, final T content) {
        this.graph = graph;
        this.content = content;
        edges = new LinkedHashSet<>();
        this.graph.addNode(this);
    }

    /**
     * Returns the hypergraph of this node.
     * @return the hypergraph of this node
     */
    public Hypergraph<T> getGraph() {
        return graph;
    }

    /**
     * Returns the content of this node.
     * @return the content of this node
     */
    public T getContent() {
        return content;
    }

    /**
     * Returns the edges which are connected with this node.
     * @return the edges which are connected with this node
     */
    public Set<HypergraphEdge<T>> getEdges() {
        return edges;
    }

    /**
     * Adds an edge to this node.
     * @param edge the edge
     */
    public void addEdge(final HypergraphEdge<T> edge) {
        edges.add(edge);
    }

    /**
     * Computes the tentative new location for this node (see Aloul, Markov, and
     * Sakallah).
     * @param nodeOrdering the node ordering for which the COG is computed
     * @return the tentative new location for this node
     */
    public double computeTentativeNewLocation(final Map<HypergraphNode<T>, Integer> nodeOrdering) {
        double newLocation = 0;
        for (final HypergraphEdge<T> edge : edges) {
            newLocation += edge.centerOfGravity(nodeOrdering);
        }
        return newLocation / edges.size();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HypergraphNode<?> that = (HypergraphNode<?>) o;
        return Objects.equals(graph, that.graph) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graph, content);
    }

    @Override
    public String toString() {
        return "HypergraphNode{" +
                "content=" + content +
                '}';
    }
}
