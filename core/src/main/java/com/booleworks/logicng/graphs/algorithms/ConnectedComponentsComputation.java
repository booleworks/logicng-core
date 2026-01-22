// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.algorithms;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * This class implements an algorithm to compute the connected components of a
 * graph.
 * @version 3.0.0
 * @since 1.2
 */
public class ConnectedComponentsComputation {

    protected ConnectedComponentsComputation() {
        // Intentionally left empty.
    }

    /**
     * Computes the set of connected components of a graph, where each component
     * is represented by a set of nodes.
     * @param graph the graph
     * @param <T>   the type of the graph content
     * @return the set of sets of nodes representing the connected components
     */
    public static <T> Set<Set<Node<T>>> compute(final Graph<T> graph) {
        final Set<Set<Node<T>>> connectedComponents = new LinkedHashSet<>();
        final Set<Node<T>> unmarkedNodes = new LinkedHashSet<>(graph.nodes());
        while (!unmarkedNodes.isEmpty()) {
            final Set<Node<T>> connectedComp = new LinkedHashSet<>();
            deepFirstSearch(unmarkedNodes.iterator().next(), connectedComp, unmarkedNodes);
            connectedComponents.add(connectedComp);
        }
        return connectedComponents;
    }

    /**
     * Split a list of formulas in their respective connected components. The
     * @param f          the formula factory to use for caching
     * @param formulas   the list of formulas
     * @param components the connected components which should be used for the
     *                   split
     * @return the list of split formulas
     */
    public static List<List<Formula>> splitFormulasByComponent(final FormulaFactory f,
                                                               final Collection<Formula> formulas,
                                                               final Set<Set<Node<Variable>>> components) {
        final Map<Set<Node<Variable>>, List<Formula>> map = new LinkedHashMap<>();
        final Map<Variable, Set<Node<Variable>>> varMap = new TreeMap<>();
        for (final Set<Node<Variable>> component : components) {
            for (final Node<Variable> variableNode : component) {
                varMap.put(variableNode.getContent(), component);
            }
        }
        for (final Formula formula : formulas) {
            final SortedSet<Variable> variables = formula.variables(f);
            if (variables.isEmpty()) {
                map.computeIfAbsent(Collections.emptySet(), l -> new ArrayList<>()).add(formula);
            } else {
                final Set<Node<Variable>> component = varMap.get(variables.first());
                if (component == null) {
                    throw new IllegalArgumentException(
                            "Could not find a component for the variable " + variables.first());
                }
                map.computeIfAbsent(component, l -> new ArrayList<>()).add(formula);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(map.values()));
    }

    protected static <T> void deepFirstSearch(final Node<T> v, final Set<Node<T>> component,
                                              final Set<Node<T>> unmarkedNodes) {
        component.add(v);
        unmarkedNodes.remove(v);
        for (final Node<T> neigh : v.getNeighbours()) {
            if (unmarkedNodes.contains(neigh)) {
                deepFirstSearch(neigh, component, unmarkedNodes);
            }
        }
    }
}
