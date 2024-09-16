// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures.ubtrees;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A data structure for storing sets with efficient sub- and superset queries.
 * C.f. `A New Method to Index and Query Sets`, Hoffmann and Koehler, 1999
 * @param <T> the type of the elements (must be comparable)
 * @version 3.0.0
 * @since 1.5.0
 */
public final class UbTree<T extends Comparable<T>> {
    private final SortedMap<T, UbNode<T>> rootNodes;

    /**
     * Constructs an empty UBTree.
     */
    public UbTree() {
        rootNodes = new TreeMap<>();
    }

    /**
     * Adds a set of comparable objects to this UBTree.
     * @param set the set of comparable objects
     */
    public void addSet(final SortedSet<T> set) {
        SortedMap<T, UbNode<T>> nodes = rootNodes;
        UbNode<T> node = null;
        for (final T element : set) {
            node = nodes.computeIfAbsent(element, UbNode::new);
            nodes = node.getChildren();
        }
        if (node != null) {
            node.setEndSet(set);
        }
    }

    /**
     * Returns the first subset of a given set in this UBTree.
     * @param set the set to search for
     * @return the first subset which is found for the given set or {@code null}
     * if there is none
     */
    public SortedSet<T> firstSubset(final SortedSet<T> set) {
        if (rootNodes.isEmpty() || set == null || set.isEmpty()) {
            return null;
        }
        return firstSubset(set, rootNodes);
    }

    /**
     * Returns all subsets of a given set in this UBTree.
     * @param set the set to search for
     * @return all subsets of the given set
     */
    public Set<SortedSet<T>> allSubsets(final SortedSet<T> set) {
        final Set<SortedSet<T>> subsets = new LinkedHashSet<>();
        allSubsets(set, rootNodes, subsets);
        return subsets;
    }

    /**
     * Returns all supersets of a given set in this UBTree.
     * @param set the set to search for
     * @return all supersets of the given set
     */
    public Set<SortedSet<T>> allSupersets(final SortedSet<T> set) {
        final Set<SortedSet<T>> supersets = new LinkedHashSet<>();
        allSupersets(set, rootNodes, supersets);
        return supersets;
    }

    /**
     * Returns all sets in this UBTree.
     * @return all sets in this UBTree
     */
    public Set<SortedSet<T>> allSets() {
        final List<UbNode<T>> allEndOfPathNodes = getAllEndOfPathNodes(rootNodes);
        final Set<SortedSet<T>> allSets = new LinkedHashSet<>();
        for (final UbNode<T> endOfPathNode : allEndOfPathNodes) {
            allSets.add(endOfPathNode.getSet());
        }
        return allSets;
    }

    /**
     * Returns all root nodes of this UBTree.
     * @return all root nodes of this UBTree
     */
    SortedMap<T, UbNode<T>> getRootNodes() {
        return rootNodes;
    }

    private SortedSet<T> firstSubset(final SortedSet<T> set, final SortedMap<T, UbNode<T>> forest) {
        final Set<UbNode<T>> nodes = getAllNodesContainingElements(set, forest);
        SortedSet<T> foundSubset = null;
        for (final UbNode<T> node : nodes) {
            if (foundSubset != null) {
                return foundSubset;
            }
            if (node.isEndOfPath()) {
                return node.getSet();
            }
            final SortedSet<T> remainingSet = new TreeSet<>(set);
            remainingSet.remove(set.first());
            foundSubset = firstSubset(remainingSet, node.getChildren());
        }
        return foundSubset;
    }

    private void allSubsets(final SortedSet<T> set, final SortedMap<T, UbNode<T>> forest,
                            final Set<SortedSet<T>> subsets) {
        final Set<UbNode<T>> nodes = getAllNodesContainingElements(set, forest);
        for (final UbNode<T> node : nodes) {
            if (node.isEndOfPath()) {
                subsets.add(node.getSet());
            }
            final SortedSet<T> remainingSet = new TreeSet<>(set);
            remainingSet.remove(set.first());
            allSubsets(remainingSet, node.getChildren(), subsets);
        }
    }

    private void allSupersets(final SortedSet<T> set, final SortedMap<T, UbNode<T>> forest,
                              final Set<SortedSet<T>> supersets) {
        final Set<UbNode<T>> nodes = getAllNodesContainingElementsLessThan(forest, set.first());
        for (final UbNode<T> node : nodes) {
            allSupersets(set, node.getChildren(), supersets);
        }
        for (final UbNode<T> node : forest.values()) {
            if (node.getElement().equals(set.first())) {
                final SortedSet<T> remainingSet = new TreeSet<>(set);
                remainingSet.remove(set.first());
                if (!remainingSet.isEmpty()) {
                    allSupersets(remainingSet, node.getChildren(), supersets);
                } else {
                    final List<UbNode<T>> allEndOfPathNodes = getAllEndOfPathNodes(node.getChildren());
                    if (node.isEndOfPath()) {
                        allEndOfPathNodes.add(node);
                    }
                    for (final UbNode<T> endOfPathNode : allEndOfPathNodes) {
                        supersets.add(endOfPathNode.getSet());
                    }
                }
            }
        }
    }

    private Set<UbNode<T>> getAllNodesContainingElements(final SortedSet<T> set, final SortedMap<T, UbNode<T>> forest) {
        final Set<UbNode<T>> nodes = new LinkedHashSet<>();
        for (final T element : set) {
            final UbNode<T> node = forest.get(element);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private Set<UbNode<T>> getAllNodesContainingElementsLessThan(final SortedMap<T, UbNode<T>> forest,
                                                                 final T element) {
        final Set<UbNode<T>> nodes = new LinkedHashSet<>();
        for (final UbNode<T> node : forest.values()) {
            if (node != null && node.getElement().compareTo(element) < 0) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private List<UbNode<T>> getAllEndOfPathNodes(final SortedMap<T, UbNode<T>> forest) {
        final List<UbNode<T>> endOfPathNodes = new ArrayList<>();
        getAllEndOfPathNodes(forest, endOfPathNodes);
        return endOfPathNodes;
    }

    private void getAllEndOfPathNodes(final SortedMap<T, UbNode<T>> forest, final List<UbNode<T>> endOfPathNodes) {
        for (final UbNode<T> node : forest.values()) {
            if (node.isEndOfPath()) {
                endOfPathNodes.add(node);
            }
            getAllEndOfPathNodes(node.getChildren(), endOfPathNodes);
        }
    }
}
