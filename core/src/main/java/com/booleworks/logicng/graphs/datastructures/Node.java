// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.datastructures;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A generic node of a graph.
 * @param <T> the element type of the node
 * @version 3.0.0
 * @since 1.2
 */
public final class Node<T> {

    private final Graph<T> graph;
    private final T content;
    private final Set<Node<T>> neighbours;

    /**
     * Constructor.
     * @param content the content of the node
     * @param graph   the graph the node will be a part of
     */
    Node(final T content, final Graph<T> graph) {
        this.content = content;
        this.graph = graph;
        neighbours = new LinkedHashSet<>();
    }

    /**
     * Adds the given node to the neighbours of this node. Both nodes must be in
     * the same graph.
     * @param o the given node
     */
    void connectTo(final Node<T> o) {
        if (!graph.equals(o.graph)) {
            throw new IllegalArgumentException("Cannot connect to nodes of two different graphs.");
        }
        if (equals(o)) {
            return;
        }
        neighbours.add(o);
    }

    /**
     * Removes the given node from the neighbours of this node.
     * @param o the given node
     */
    void disconnectFrom(final Node<T> o) {
        neighbours.remove(o);
    }

    /**
     * Returns the content of the node.
     * @return the content of the node
     */
    public T getContent() {
        return content;
    }

    /**
     * Returns the neighbours of the node.
     * @return the neighbours of the node
     */
    public Set<Node<T>> getNeighbours() {
        return new LinkedHashSet<>(neighbours);
    }

    /**
     * Returns the graph to which the node belongs.
     * @return the node's graph
     */
    public Graph<T> getGraph() {
        return graph;
    }

    @Override
    public int hashCode() {
        int result = graph.hashCode();
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Node<?> node = (Node<?>) o;
        return graph.equals(node.graph) && Objects.equals(content, node.content);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Node{content=").append(content).append(", neighbours:");
        for (final Node<T> neighbour : neighbours) {
            sb.append(neighbour.getContent()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
