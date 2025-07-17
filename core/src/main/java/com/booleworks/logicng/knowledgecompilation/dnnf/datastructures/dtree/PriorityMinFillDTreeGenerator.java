// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_DTREE_MIN_FILL_GRAPH_INITIALIZED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_DTREE_MIN_FILL_NEW_ITERATION;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * A DTree generator using the min-fill heuristic.
 * @version 3.0.0
 * @since 2.0.0
 */
public class PriorityMinFillDTreeGenerator extends EliminatingOrderDTreeGenerator {

    private final List<Set<Variable>> priorityPartition;

    public PriorityMinFillDTreeGenerator(final List<Set<Variable>> priorityPartition) {
        this.priorityPartition = priorityPartition;
    }

    @Override
    public LngResult<DTree> generate(final FormulaFactory f, final Formula cnf,
                                     final ComputationHandler handler) {
        if (!handler.shouldResume(ComputationStartedEvent.DTREE_GENERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.DTREE_GENERATION_STARTED);
        }
        final Graph graph = new Graph(f, cnf, priorityPartition);
        if (!handler.shouldResume(DNNF_DTREE_MIN_FILL_GRAPH_INITIALIZED)) {
            return LngResult.canceled(DNNF_DTREE_MIN_FILL_GRAPH_INITIALIZED);
        }
        final LngResult<List<Variable>> minFillOrdering = graph.getMinFillOrdering(handler);
        if (!minFillOrdering.isSuccess()) {
            return LngResult.canceled(minFillOrdering.getCancelCause());
        }
        return generateWithEliminatingOrder(f, cnf, minFillOrdering.getResult(), handler);
    }

    /**
     * Undirected Graph
     */
    public static class Graph {
        protected final int numberOfVertices;

        protected final int[] priorities;

        /**
         * The adjacency matrix (which is symmetric since the graph is
         * undirected)
         */
        protected final boolean[][] adjMatrix;

        /**
         * The list of vertices
         */
        protected final List<Variable> vertices;

        /**
         * The edges of the graph as a list of edges per node ({{2,3},{1},{1}}
         * means that there are the edges 1-2 and 1-3)
         */
        protected final List<Set<Integer>> edgeList;

        /**
         * Computes the DTree from the given CNF.
         * @param f   the formula factory to use for caching
         * @param cnf the CNF
         */
        public Graph(final FormulaFactory f, final Formula cnf, final List<Set<Variable>> priorityVariables) {
            /* build vertices */
            numberOfVertices = cnf.variables(f).size();
            vertices = new ArrayList<>(numberOfVertices);
            priorities = new int[numberOfVertices];
            final Map<Literal, Integer> varToIndex = new HashMap<>();
            int index = 0;
            for (final Variable variable : cnf.variables(f)) {
                vertices.add(variable);
                varToIndex.put(variable, index++);
            }
            for (int i = 0; i < priorityVariables.size(); i++) {
                for (final Variable variable : priorityVariables.get(i)) {
                    if (varToIndex.containsKey(variable)) {
                        priorities[varToIndex.get(variable)] = i;
                    }
                }

            }

            /* build edge list and adjacency matrix */
            {
                adjMatrix = new boolean[numberOfVertices][numberOfVertices];
            }
            edgeList = new ArrayList<>(numberOfVertices);
            for (int i = 0; i < numberOfVertices; i++) {
                edgeList.add(new LinkedHashSet<>());
            }

            for (final Formula clause : cnf) {
                final SortedSet<Variable> variables = clause.variables(f);
                final int[] varNums = new int[variables.size()];
                index = 0;
                for (final Literal var : variables) {
                    varNums[index++] = varToIndex.get(var);
                }
                for (int i = 0; i < varNums.length; i++) {
                    for (int j = i + 1; j < varNums.length; j++) {
                        edgeList.get(varNums[i]).add(varNums[j]);
                        edgeList.get(varNums[j]).add(varNums[i]);
                        adjMatrix[varNums[i]][varNums[j]] = true;
                        adjMatrix[varNums[j]][varNums[i]] = true;
                    }
                }
            }
        }

        protected List<LngIntVector> getCopyOfEdgeList() {
            final List<LngIntVector> result = new ArrayList<>();
            for (final Set<Integer> edge : edgeList) {
                result.add(LngIntVector.of(edge.stream().mapToInt(i -> i).toArray()));
            }
            return result;
        }

        protected boolean[][] getCopyOfAdjMatrix() {
            final boolean[][] result = new boolean[numberOfVertices][numberOfVertices];
            for (int i = 0; i < numberOfVertices; i++) {
                result[i] = Arrays.copyOf(adjMatrix[i], numberOfVertices);
            }
            return result;
        }

        protected LngResult<List<Variable>> getMinFillOrdering(final ComputationHandler handler) {
            final boolean[][] fillAdjMatrix = getCopyOfAdjMatrix();
            final List<LngIntVector> fillEdgeList = getCopyOfEdgeList();

            final Variable[] ordering = new Variable[numberOfVertices];
            final boolean[] processed = new boolean[numberOfVertices];
            int treewidth = 0;

            for (int iteration = 0; iteration < numberOfVertices; iteration++) {
                if (!handler.shouldResume(DNNF_DTREE_MIN_FILL_NEW_ITERATION)) {
                    return LngResult.canceled(DNNF_DTREE_MIN_FILL_NEW_ITERATION);
                }
                final LngIntVector possiblyBestVertices = new LngIntVector();
                int minEdges = Integer.MAX_VALUE;
                int minPriority = Integer.MAX_VALUE;
                for (int currentVertex = 0; currentVertex < numberOfVertices; currentVertex++) {
                    if (processed[currentVertex]) {
                        continue;
                    }
                    int edgesAdded = 0;
                    final LngIntVector neighborList = fillEdgeList.get(currentVertex);
                    for (int i = 0; i < neighborList.size(); i++) {
                        final int firstNeighbor = neighborList.get(i);
                        if (processed[firstNeighbor]) {
                            continue;
                        }
                        for (int j = i + 1; j < neighborList.size(); j++) {
                            final int secondNeighbor = neighborList.get(j);
                            if (processed[secondNeighbor]) {
                                continue;
                            }
                            if (!fillAdjMatrix[firstNeighbor][secondNeighbor]) {
                                edgesAdded++;
                            }
                        }
                    }
                    final int priority = priorities[currentVertex];
                    if (edgesAdded < minEdges || (edgesAdded == minEdges && priority < minPriority)) {
                        minEdges = edgesAdded;
                        minPriority = priority;
                        possiblyBestVertices.clear();
                        possiblyBestVertices.push(currentVertex);
                    } else if (edgesAdded == minEdges && priority == minPriority) {
                        possiblyBestVertices.push(currentVertex);
                    }
                }

                // or choose randomly
                final int bestVertex = possiblyBestVertices.get(0);

                final LngIntVector neighborList = fillEdgeList.get(bestVertex);
                for (int i = 0; i < neighborList.size(); i++) {
                    final int firstNeighbor = neighborList.get(i);
                    if (processed[firstNeighbor]) {
                        continue;
                    }
                    for (int j = i + 1; j < neighborList.size(); j++) {
                        final int secondNeighbor = neighborList.get(j);
                        if (processed[secondNeighbor]) {
                            continue;
                        }
                        if (!fillAdjMatrix[firstNeighbor][secondNeighbor]) {
                            fillAdjMatrix[firstNeighbor][secondNeighbor] = true;
                            fillAdjMatrix[secondNeighbor][firstNeighbor] = true;
                            fillEdgeList.get(firstNeighbor).push(secondNeighbor);
                            fillEdgeList.get(secondNeighbor).push(firstNeighbor);
                        }
                    }
                }

                int currentNumberOfEdges = 0;
                for (int k = 0; k < numberOfVertices; k++) {
                    if (k != bestVertex && !processed[k] && fillAdjMatrix[bestVertex][k]) {
                        currentNumberOfEdges++;
                    }
                }
                if (treewidth < currentNumberOfEdges) {
                    treewidth = currentNumberOfEdges;
                }

                processed[bestVertex] = true;
                ordering[iteration] = vertices.get(bestVertex);
            }
            return LngResult.of(Arrays.asList(ordering));
        }
    }
}
