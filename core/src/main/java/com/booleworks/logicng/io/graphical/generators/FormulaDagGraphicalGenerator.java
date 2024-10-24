// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.functions.LiteralsFunction;
import com.booleworks.logicng.io.graphical.GraphicalEdge;
import com.booleworks.logicng.io.graphical.GraphicalNode;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * The graphical generator for representations of DAGs (directed acyclic graphs)
 * of a formula.
 * @version 2.4.0
 * @since 2.4.0
 */
public class FormulaDagGraphicalGenerator extends GraphicalGenerator<Formula> {

    /**
     * Constructs a new generator with the given builder's configuration.
     * @param builder the builder
     */
    FormulaDagGraphicalGenerator(final GraphicalGeneratorBuilder<FormulaDagGraphicalGenerator, Formula> builder) {
        super(builder.backgroundColor, builder.alignTerminals, builder.defaultEdgeStyle, builder.defaultNodeStyle,
                builder.nodeStyleMapper,
                builder.labelMapper, builder.edgeMapper);
    }

    /**
     * Returns the builder for this generator.
     * @return the builder
     */
    public static GraphicalGeneratorBuilder<FormulaDagGraphicalGenerator, Formula> builder() {
        return new GraphicalGeneratorBuilder<>(FormulaDagGraphicalGenerator::new);
    }

    /**
     * Translates a given formula's DAG into its graphical representation.
     * @param formula the formula
     * @return the graphical representation
     */
    public GraphicalRepresentation translate(final Formula formula) {
        final Map<Formula, GraphicalNode> nodes = new HashMap<>();
        final GraphicalRepresentation graphicalRepresentation =
                new GraphicalRepresentation(alignTerminals, true, backgroundColor);
        final LiteralsFunction lf = new LiteralsFunction(formula.getFactory(), null);
        for (final Literal lit : formula.apply(lf)) {
            final String label = (lit.getPhase() ? "" : "¬") + lit.getName();
            final GraphicalNode literalNode = new GraphicalNode(ID + nodes.size(), label, true, nodeStyle(lit));
            graphicalRepresentation.addNode(literalNode);
            nodes.put(lit, literalNode);
        }
        walkFormula(formula, graphicalRepresentation, nodes);
        return graphicalRepresentation;
    }

    private GraphicalNode walkFormula(final Formula formula, final GraphicalRepresentation graphicalRepresentation,
                                      final Map<Formula, GraphicalNode> nodes) {
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case PREDICATE:
                final Pair<GraphicalNode, Boolean> constPair =
                        addNode(formula, formula.toString(), true, graphicalRepresentation, nodes);
                return constPair.getFirst();
            case LITERAL:
                // since this is a literal, it has to be already present
                return nodes.get(formula);
            case PBC:
                return walkPBConstraint((PbConstraint) formula, graphicalRepresentation, nodes);
            case NOT:
                return walkNotFormula((Not) formula, graphicalRepresentation, nodes);
            case IMPL:
            case EQUIV:
                return walkBinaryFormula((BinaryOperator) formula, graphicalRepresentation, nodes);
            case AND:
            case OR:
                return walkNaryFormula((NAryOperator) formula, graphicalRepresentation, nodes);
            default:
                throw new IllegalArgumentException("Encountered unknown formula formula type " + formula.getType());
        }
    }

    private GraphicalNode walkPBConstraint(final PbConstraint pbc,
                                           final GraphicalRepresentation graphicalRepresentation,
                                           final Map<Formula, GraphicalNode> nodes) {
        final Pair<GraphicalNode, Boolean> pbPair = addNode(pbc, pbc.toString(), false, graphicalRepresentation, nodes);
        if (!pbPair.getSecond()) {
            for (final Formula operand : pbc.getOperands()) {
                // since this is a literal, it has to be already present
                final GraphicalNode literalNode = nodes.get(operand);
                graphicalRepresentation
                        .addEdge(new GraphicalEdge(pbPair.getFirst(), literalNode, edgeStyle(pbc, operand)));
            }
        }
        return pbPair.getFirst();
    }

    private GraphicalNode walkNotFormula(final Not not, final GraphicalRepresentation graphicalRepresentation,
                                         final Map<Formula, GraphicalNode> nodes) {
        final Pair<GraphicalNode, Boolean> pair = addNode(not, "¬", false, graphicalRepresentation, nodes);
        if (!pair.getSecond()) {
            final GraphicalNode operandNode = walkFormula(not.getOperand(), graphicalRepresentation, nodes);
            graphicalRepresentation
                    .addEdge(new GraphicalEdge(pair.getFirst(), operandNode, edgeStyle(not, not.getOperand())));
        }
        return pair.getFirst();
    }

    private GraphicalNode walkBinaryFormula(final BinaryOperator op,
                                            final GraphicalRepresentation graphicalRepresentation,
                                            final Map<Formula, GraphicalNode> nodes) {
        final boolean isImpl = op.getType() == FType.IMPL;
        final String label = isImpl ? "⇒" : "⇔";
        final Pair<GraphicalNode, Boolean> pair = addNode(op, label, false, graphicalRepresentation, nodes);
        if (!pair.getSecond()) {
            final GraphicalNode leftNode = walkFormula(op.getLeft(), graphicalRepresentation, nodes);
            final GraphicalNode rightNode = walkFormula(op.getRight(), graphicalRepresentation, nodes);
            graphicalRepresentation
                    .addEdge(new GraphicalEdge(pair.getFirst(), leftNode, isImpl ? "l" : null, edgeStyle(op, op.getLeft())));
            graphicalRepresentation.addEdge(
                    new GraphicalEdge(pair.getFirst(), rightNode, isImpl ? "r" : null, edgeStyle(op, op.getRight())));
        }
        return pair.getFirst();
    }

    private GraphicalNode walkNaryFormula(final NAryOperator op, final GraphicalRepresentation graphicalRepresentation,
                                          final Map<Formula, GraphicalNode> nodes) {
        final String label = op.getType() == FType.AND ? "∧" : "∨";
        final Pair<GraphicalNode, Boolean> pair = addNode(op, label, false, graphicalRepresentation, nodes);
        if (!pair.getSecond()) {
            for (final Formula operand : op) {
                final GraphicalNode operandNode = walkFormula(operand, graphicalRepresentation, nodes);
                graphicalRepresentation.addEdge(new GraphicalEdge(pair.getFirst(), operandNode, edgeStyle(op, operand)));
            }
        }
        return pair.getFirst();
    }

    private Pair<GraphicalNode, Boolean> addNode(final Formula formula, final String defaultLabel,
                                                 final boolean terminal,
                                                 final GraphicalRepresentation graphicalRepresentation,
                                                 final Map<Formula, GraphicalNode> nodes) {
        GraphicalNode node = nodes.get(formula);
        if (node == null) {
            node = new GraphicalNode(ID + nodes.size(), labelOrDefault(formula, defaultLabel), terminal,
                    nodeStyle(formula));
            graphicalRepresentation.addNode(node);
            nodes.put(formula, node);
            return new Pair<>(node, false);
        } else {
            return new Pair<>(node, true);
        }
    }
}
