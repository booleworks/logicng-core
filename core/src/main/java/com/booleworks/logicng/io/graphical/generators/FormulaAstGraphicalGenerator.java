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
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.io.graphical.GraphicalEdge;
import com.booleworks.logicng.io.graphical.GraphicalNode;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;

/**
 * The graphical generator for representations of ASTs (abstract syntax trees)
 * of a formula.
 * @version 2.4.0
 * @since 2.4.0
 */
public class FormulaAstGraphicalGenerator extends GraphicalGenerator<Formula> {

    /**
     * Constructs a new generator with the given builder's configuration.
     * @param builder the builder
     */
    FormulaAstGraphicalGenerator(final GraphicalGeneratorBuilder<FormulaAstGraphicalGenerator, Formula> builder) {
        super(builder.backgroundColor, builder.alignTerminals, builder.defaultEdgeStyle, builder.defaultNodeStyle,
                builder.nodeStyleMapper,
                builder.labelMapper, builder.edgeMapper);
    }

    /**
     * Returns the builder for this generator.
     * @return the builder
     */
    public static GraphicalGeneratorBuilder<FormulaAstGraphicalGenerator, Formula> builder() {
        return new GraphicalGeneratorBuilder<>(FormulaAstGraphicalGenerator::new);
    }

    /**
     * Translates a given formula's AST into its graphical representation.
     * @param formula the formula
     * @return the graphical representation
     */
    public GraphicalRepresentation translate(final Formula formula) {
        final GraphicalRepresentation graphicalRepresentation =
                new GraphicalRepresentation(alignTerminals, true, backgroundColor);
        walkFormula(formula, graphicalRepresentation);
        return graphicalRepresentation;
    }

    private GraphicalNode walkFormula(final Formula formula, final GraphicalRepresentation graphicalRepresentation) {
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
                return walkAtomicFormula(formula, graphicalRepresentation);
            case PBC:
                return walkPBConstraint((PBConstraint) formula, graphicalRepresentation);
            case NOT:
                return walkNotFormula((Not) formula, graphicalRepresentation);
            case IMPL:
            case EQUIV:
                return walkBinaryFormula((BinaryOperator) formula, graphicalRepresentation);
            case AND:
            case OR:
                return walkNaryFormula((NAryOperator) formula, graphicalRepresentation);
            default:
                throw new IllegalArgumentException("Encountered unknown formula type " + formula.getType());
        }
    }

    private GraphicalNode walkAtomicFormula(final Formula formula,
                                            final GraphicalRepresentation graphicalRepresentation) {
        final String label = formula.getType() == FType.LITERAL ? litString((Literal) formula) : formula.toString();
        return addNode(formula, label, true, graphicalRepresentation);
    }

    private GraphicalNode walkPBConstraint(final PBConstraint pbc,
                                           final GraphicalRepresentation graphicalRepresentation) {
        final GraphicalNode pbNode = addNode(pbc, pbc.toString(), false, graphicalRepresentation);
        for (final Literal operand : pbc.getOperands()) {
            final GraphicalNode literalNode = addNode(operand, litString(operand), true, graphicalRepresentation);
            graphicalRepresentation.addEdge(new GraphicalEdge(pbNode, literalNode, edgeStyle(pbc, operand)));
        }
        return pbNode;
    }

    private GraphicalNode walkNotFormula(final Not not, final GraphicalRepresentation graphicalRepresentation) {
        final GraphicalNode node = addNode(not, "¬", false, graphicalRepresentation);
        final GraphicalNode operandNode = walkFormula(not.getOperand(), graphicalRepresentation);
        graphicalRepresentation.addEdge(new GraphicalEdge(node, operandNode, edgeStyle(not, not.getOperand())));
        return node;
    }

    private GraphicalNode walkBinaryFormula(final BinaryOperator op,
                                            final GraphicalRepresentation graphicalRepresentation) {
        final boolean isImpl = op.getType() == FType.IMPL;
        final String label = isImpl ? "⇒" : "⇔";
        final GraphicalNode node = addNode(op, label, false, graphicalRepresentation);
        final GraphicalNode leftNode = walkFormula(op.getLeft(), graphicalRepresentation);
        final GraphicalNode rightNode = walkFormula(op.getRight(), graphicalRepresentation);
        graphicalRepresentation
                .addEdge(new GraphicalEdge(node, leftNode, isImpl ? "l" : null, edgeStyle(op, op.getLeft())));
        graphicalRepresentation
                .addEdge(new GraphicalEdge(node, rightNode, isImpl ? "r" : null, edgeStyle(op, op.getRight())));
        return node;
    }

    private GraphicalNode walkNaryFormula(final NAryOperator op,
                                          final GraphicalRepresentation graphicalRepresentation) {
        final String label = op.getType() == FType.AND ? "∧" : "∨";
        final GraphicalNode node = addNode(op, label, false, graphicalRepresentation);
        for (final Formula operand : op) {
            final GraphicalNode operandNode = walkFormula(operand, graphicalRepresentation);
            graphicalRepresentation.addEdge(new GraphicalEdge(node, operandNode, edgeStyle(op, operand)));
        }
        return node;
    }

    private GraphicalNode addNode(final Formula formula, final String defaultLabel, final boolean terminal,
                                  final GraphicalRepresentation graphicalRepresentation) {
        final GraphicalNode node =
                new GraphicalNode(ID + graphicalRepresentation.getNodes().size(), labelOrDefault(formula, defaultLabel),
                        terminal, nodeStyle(formula));
        graphicalRepresentation.addNode(node);
        return node;
    }

    private static String litString(final Literal literal) {
        return (literal.getPhase() ? "" : "¬") + literal.getName();
    }
}
