// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.generators;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.datastructures.Hypergraph;
import com.booleworks.logicng.graphs.datastructures.HypergraphNode;
import com.booleworks.logicng.predicates.CnfPredicate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * A generator for hyper-graphs from formulas.
 * @version 3.0.0
 * @since 1.4.0
 */
public class HypergraphGenerator {

    /**
     * Generates a hyper-graph from a CNF given as a list of clauses. Each
     * variable is represented by a node in the hyper-graph, each clause is
     * represented by a hyper-edge between all variables of the clause.
     * @param f   the formula factory to use for caching
     * @param cnf the list of clauses of the CNF for the hyper-graph
     * @return the hyper-graph for the CNF formula
     */
    public static Hypergraph<Variable> fromCnf(final FormulaFactory f, final Formula... cnf) {
        return fromCnf(f, Arrays.asList(cnf));
    }

    /**
     * Generates a hyper-graph from a CNF given as a list of clauses. Each
     * variable is represented by a node in the hyper-graph, each clause is
     * represented by a hyper-edge between all variables of the clause.
     * @param f   the formula factory to use for caching
     * @param cnf the list of clauses of the CNF for the hyper-graph
     * @return the hyper-graph for the CNF formula
     */
    public static Hypergraph<Variable> fromCnf(final FormulaFactory f, final List<Formula> cnf) {
        final Hypergraph<Variable> hypergraph = new Hypergraph<>();
        final Map<Variable, HypergraphNode<Variable>> nodes = new HashMap<>();
        for (final Formula clause : cnf) {
            switch (clause.getType()) {
                case PBC:
                case EQUIV:
                case IMPL:
                case NOT:
                case AND:
                case PREDICATE:
                    throw new IllegalStateException("Unexpected element in clause: " + clause);
                case LITERAL:
                case OR:
                    addClause(f, clause, hypergraph, nodes);
                    break;
            }
        }
        return hypergraph;
    }

    /**
     * Generates a hyper-graph from a CNF. Each variable is represented by a
     * node in the hyper-graph, each clause is represented by a hyper-edge
     * between all variables of the clause.
     * @param f   the formula factory to use for caching
     * @param cnf the CNF formula for the hyper-graph
     * @return the hyper-graph for the CNF formula
     */
    public static Hypergraph<Variable> fromCnf(final FormulaFactory f, final Formula cnf) {
        if (!cnf.holds(new CnfPredicate(cnf.getFactory(), null))) {
            throw new IllegalArgumentException("Cannot generate a hypergraph from a non-cnf formula");
        }
        final Hypergraph<Variable> hypergraph = new Hypergraph<>();
        final Map<Variable, HypergraphNode<Variable>> nodes = new HashMap<>();
        switch (cnf.getType()) {
            case PBC:
            case EQUIV:
            case IMPL:
            case NOT:
            case PREDICATE:
                throw new IllegalStateException("Unexpected element in CNF: " + cnf);
            case LITERAL:
            case OR:
                addClause(f, cnf, hypergraph, nodes);
                break;
            case AND:
                for (final Formula clause : cnf) {
                    addClause(f, clause, hypergraph, nodes);
                }
                break;
        }
        return hypergraph;
    }

    protected static void addClause(final FormulaFactory f, final Formula formula,
                                    final Hypergraph<Variable> hypergraph,
                                    final Map<Variable, HypergraphNode<Variable>> nodes) {
        assert formula.getType() == FType.LITERAL || formula.getType() == FType.OR;
        final SortedSet<Variable> variables = formula.variables(f);
        final Set<HypergraphNode<Variable>> clause = new LinkedHashSet<>();
        for (final Variable variable : variables) {
            HypergraphNode<Variable> node = nodes.get(variable);
            if (node == null) {
                node = new HypergraphNode<>(hypergraph, variable);
                nodes.put(variable, node);
            }
            clause.add(node);
        }
        hypergraph.addEdge(clause);
    }
}
