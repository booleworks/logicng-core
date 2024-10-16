// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.ubtrees;

import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * A node in a UBTree, holding a comparable element.
 * @param <T> The element type of the node, must be comparable
 * @version 3.0.0
 * @since 1.5.0
 */
public final class UbNode<T extends Comparable<T>> {

    private final T element;
    private final SortedMap<T, UbNode<T>> children;
    private SortedSet<T> set;

    /**
     * Constructs a new UB Tree node with the given element.
     * @param element the node's element
     */
    UbNode(final T element) {
        this.element = element;
        children = new TreeMap<>();
    }

    /**
     * Returns the element of this node.
     * @return the element of this node.
     */
    T getElement() {
        return element;
    }

    /**
     * Returns the set of this node. If this node is a terminal node, it holds a
     * set of the UB Tree. In this case these methods returns this set,
     * otherwise it returns {@code null}.
     * @return the set of this node if it is a terminal node, {@code null}
     * otherwise
     */
    SortedSet<T> getSet() {
        return set;
    }

    /**
     * Returns all children of this node.
     * @return a mapping from element to its node - all of which are children of
     * the current node
     */
    SortedMap<T, UbNode<T>> getChildren() {
        return children;
    }

    /**
     * Returns whether this node is a terminal node or not.
     * @return {@code true} if this is a terminal node, {@code false} otherwise
     */
    boolean isEndOfPath() {
        return set != null;
    }

    /**
     * Sets a set for this node and therefore this node is a terminal node.
     * @param set the set for this node
     */
    void setEndSet(final SortedSet<T> set) {
        this.set = set;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UbNode<?> ubNode = (UbNode<?>) o;
        return Objects.equals(element, ubNode.element) &&
                Objects.equals(children, ubNode.children) &&
                Objects.equals(set, ubNode.set);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, children, set);
    }

    @Override
    public String toString() {
        return "UbNode{" +
                "element=" + element +
                ", children=" + children +
                ", set=" + set +
                '}';
    }
}
