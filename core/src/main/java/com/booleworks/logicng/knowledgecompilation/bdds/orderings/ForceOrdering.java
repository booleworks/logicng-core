// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.datastructures.Hypergraph;
import com.booleworks.logicng.graphs.datastructures.HypergraphNode;
import com.booleworks.logicng.graphs.generators.HypergraphGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Simple implementation of the FORCE BDD variable ordering due to Aloul,
 * Markov, and Sakallah. This ordering only works for CNF formulas. A formula
 * has to be converted to CNF before this ordering is called.
 * @version 2.0.0
 * @since 1.4.0
 */
public final class ForceOrdering implements VariableOrderingProvider {

    private static final Comparator<? super Map.Entry<HypergraphNode<Variable>, Double>> COMPARATOR =
            Map.Entry.comparingByValue();

    private final DfsOrdering dfsOrdering = new DfsOrdering();

    @Override
    public List<Variable> getOrder(final FormulaFactory f, final Formula formula) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables(f));
        final Formula nnf = formula.nnf(f);
        originalVariables.addAll(nnf.variables(f));
        final Formula cnf = nnf.cnf(f);
        final Hypergraph<Variable> hypergraph = HypergraphGenerator.fromCnf(f, cnf);
        final Map<Variable, HypergraphNode<Variable>> nodes = new HashMap<>();
        for (final HypergraphNode<Variable> node : hypergraph.getNodes()) {
            nodes.put(node.getContent(), node);
        }
        final List<Variable> ordering = force(f, cnf, hypergraph, nodes).stream().filter(originalVariables::contains)
                .collect(Collectors.toList());
        originalVariables.stream().filter(v -> !ordering.contains(v)).forEach(ordering::add);
        return ordering;
    }

    private List<Variable> force(final FormulaFactory f, final Formula formula, final Hypergraph<Variable> hypergraph,
                                 final Map<Variable, HypergraphNode<Variable>> nodes) {
        final LinkedHashMap<HypergraphNode<Variable>, Integer> initialOrdering =
                createInitialOrdering(f, formula, nodes);
        LinkedHashMap<HypergraphNode<Variable>, Integer> lastOrdering;
        LinkedHashMap<HypergraphNode<Variable>, Integer> currentOrdering = initialOrdering;
        do {
            lastOrdering = currentOrdering;
            final LinkedHashMap<HypergraphNode<Variable>, Double> newLocations = new LinkedHashMap<>();
            for (final HypergraphNode<Variable> node : hypergraph.getNodes()) {
                newLocations.put(node, node.computeTentativeNewLocation(lastOrdering));
            }
            currentOrdering = orderingFromTentativeNewLocations(newLocations);
        } while (shouldProceed(lastOrdering, currentOrdering));
        final Variable[] ordering = new Variable[currentOrdering.size()];
        for (final Map.Entry<HypergraphNode<Variable>, Integer> entry : currentOrdering.entrySet()) {
            ordering[entry.getValue()] = entry.getKey().getContent();
        }
        return Arrays.asList(ordering);
    }

    /**
     * Creates an initial ordering for the variables based on a DFS.
     * @param formula the CNF formula
     * @param nodes   the variable to hyper-graph node mapping
     * @return the initial variable ordering
     */
    private LinkedHashMap<HypergraphNode<Variable>, Integer>
    createInitialOrdering(final FormulaFactory f, final Formula formula,
                          final Map<Variable, HypergraphNode<Variable>> nodes) {
        final LinkedHashMap<HypergraphNode<Variable>, Integer> initialOrdering = new LinkedHashMap<>();
        final List<Variable> dfsOrder = dfsOrdering.getOrder(f, formula);
        for (int i = 0; i < dfsOrder.size(); i++) {
            initialOrdering.put(nodes.get(dfsOrder.get(i)), i);
        }
        return initialOrdering;
    }

    /**
     * Generates a new integer ordering from tentative new locations of nodes
     * with the double weighting.
     * @param newLocations the tentative new locations
     * @return the new integer ordering
     */
    private LinkedHashMap<HypergraphNode<Variable>, Integer>
    orderingFromTentativeNewLocations(final LinkedHashMap<HypergraphNode<Variable>, Double> newLocations) {
        final LinkedHashMap<HypergraphNode<Variable>, Integer> ordering = new LinkedHashMap<>();
        final List<Map.Entry<HypergraphNode<Variable>, Double>> list = new ArrayList<>(newLocations.entrySet());
        list.sort(COMPARATOR);
        int count = 0;
        for (final Map.Entry<HypergraphNode<Variable>, Double> entry : list) {
            ordering.put(entry.getKey(), count++);
        }
        return ordering;
    }

    /**
     * The cancel criteria for the FORCE algorithm.
     * @param lastOrdering    the ordering of the last step
     * @param currentOrdering the ordering of the current step
     * @return {@code true} if the algorithm should proceed, {@code false} if it
     * should stop
     */
    private boolean shouldProceed(final Map<HypergraphNode<Variable>, Integer> lastOrdering,
                                  final Map<HypergraphNode<Variable>, Integer> currentOrdering) {
        return !lastOrdering.equals(currentOrdering);
    }
}
