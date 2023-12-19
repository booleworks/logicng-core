// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import static com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel.BDD_FALSE;
import static com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel.BDD_TRUE;

import com.booleworks.logicng.io.graphical.GraphicalColor;
import com.booleworks.logicng.io.graphical.GraphicalEdge;
import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.io.graphical.GraphicalNode;
import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDOperations;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * The graphical generator for representations of BDDs {@link BDD}.
 * @version 2.4.0
 * @since 2.4.0
 */
public class BddGraphicalGenerator extends GraphicalGenerator<Integer> {

    public static final GraphicalNodeStyle DEFAULT_TRUE_NODE_STYLE =
            GraphicalNodeStyle.rectangle(GraphicalColor.GREEN, GraphicalColor.WHITE, GraphicalColor.GREEN);
    public static final GraphicalNodeStyle DEFAULT_FALSE_NODE_STYLE =
            GraphicalNodeStyle.rectangle(GraphicalColor.RED, GraphicalColor.WHITE, GraphicalColor.RED);
    public static final GraphicalEdgeStyle DEFAULT_TRUE_EDGE_STYLE = GraphicalEdgeStyle.solid(GraphicalColor.GREEN);
    public static final GraphicalEdgeStyle DEFAULT_FALSE_EDGE_STYLE = GraphicalEdgeStyle.dotted(GraphicalColor.RED);

    private final GraphicalEdgeStyle negativeEdgeStyle;
    private final EdgeStyleMapper<Integer> negativeEdgeStyleMapper;

    /**
     * Constructs a new generator with the given builder's configuration.
     * @param builder the builder
     */
    BddGraphicalGenerator(final GraphicalGeneratorBuilder<BddGraphicalGenerator, Integer> builder) {
        super(builder.backgroundColor, builder.alignTerminals, builder.defaultEdgeStyle, builder.defaultNodeStyle,
                builder.nodeStyleMapper,
                builder.labelMapper, builder.edgeMapper);
        final BddGraphicalGeneratorBuilder bddBuilder = (BddGraphicalGeneratorBuilder) builder;
        negativeEdgeStyle = bddBuilder.negativeEdgeStyle;
        negativeEdgeStyleMapper = bddBuilder.negativeEdgeMapper;
    }

    /**
     * Returns the builder for this generator.
     * @return the builder
     */
    public static BddGraphicalGeneratorBuilder builder() {
        return new BddGraphicalGeneratorBuilder(BddGraphicalGenerator::new);
    }

    /**
     * Translates a given BDD into its graphical representation.
     * @param bdd the BDD
     * @return the graphical representation
     */
    public GraphicalRepresentation translate(final BDD bdd) {
        final Map<Integer, GraphicalNode> index2Node = new TreeMap<>();
        final GraphicalRepresentation graphicalRepresentation =
                new GraphicalRepresentation(alignTerminals, true, backgroundColor);
        if (!bdd.isTautology()) {
            final GraphicalNode falseNode =
                    new GraphicalNode(ID + BDD_FALSE, labelOrDefault(BDD_FALSE, "false"), true, nodeStyle(BDD_FALSE));
            graphicalRepresentation.addNode(falseNode);
            index2Node.put(BDD_FALSE, falseNode);
        }
        if (!bdd.isContradiction()) {
            final GraphicalNode trueNode =
                    new GraphicalNode(ID + BDD_TRUE, labelOrDefault(BDD_TRUE, "true"), true, nodeStyle(BDD_TRUE));
            graphicalRepresentation.addNode(trueNode);
            index2Node.put(BDD_TRUE, trueNode);
        }
        for (final int[] internalNode : new BDDOperations(bdd.underlyingKernel()).allNodes(bdd.index())) {
            final int index = internalNode[0];
            final String defaultLabel = bdd.underlyingKernel().getVariableForIndex(internalNode[1]).name();
            addNode(index, labelOrDefault(index, defaultLabel), graphicalRepresentation, index2Node);
        }
        for (final int[] internalNode : new BDDOperations(bdd.underlyingKernel()).allNodes(bdd.index())) {
            final int index = internalNode[0];
            final int lowIndex = internalNode[2];
            final int highIndex = internalNode[3];
            final GraphicalNode node = index2Node.get(index);
            final GraphicalNode lowNode = index2Node.get(lowIndex);
            final GraphicalNode highNode = index2Node.get(highIndex);
            graphicalRepresentation.addEdge(new GraphicalEdge(node, lowNode, negativeEdgeStyle(index, lowIndex)));
            graphicalRepresentation.addEdge(new GraphicalEdge(node, highNode, edgeStyle(index, highIndex)));
        }
        return graphicalRepresentation;
    }

    private void addNode(final int index, final String label, final GraphicalRepresentation graphicalRepresentation,
                         final Map<Integer, GraphicalNode> index2Node) {
        GraphicalNode node = index2Node.get(index);
        if (node == null) {
            node = new GraphicalNode(ID + index, label, false, nodeStyle(index));
            graphicalRepresentation.addNode(node);
            index2Node.put(index, node);
        }
    }

    private GraphicalEdgeStyle negativeEdgeStyle(final Integer source, final Integer destination) {
        return negativeEdgeStyleMapper != null ? negativeEdgeStyleMapper.computeStyle(source, destination)
                : negativeEdgeStyle;
    }

    /**
     * An extension of the builder for graphical generators to include BDD
     * specific values.
     * <p>
     * Since only the additional methods of this builder can return an object of
     * the type {@link BddGraphicalGeneratorBuilder}, you have to configure all
     * BDD-specific values of this builder _before_ you configure the values of
     * the super builder {@link GraphicalGeneratorBuilder}.
     * @version 2.4.0
     * @since 2.4.0
     */
    public static class BddGraphicalGeneratorBuilder extends GraphicalGeneratorBuilder<BddGraphicalGenerator, Integer> {

        private GraphicalNodeStyle defaultTrueNodeStyle = DEFAULT_TRUE_NODE_STYLE;
        private GraphicalNodeStyle defaultFalseNodeStyle = DEFAULT_FALSE_NODE_STYLE;
        private GraphicalEdgeStyle negativeEdgeStyle = DEFAULT_FALSE_EDGE_STYLE;
        private EdgeStyleMapper<Integer> negativeEdgeMapper = null;

        /**
         * Constructs a new builder with the given constructor for the graphical
         * generator.
         * @param constructor the constructor for the graphical generator
         */
        BddGraphicalGeneratorBuilder(final Function<GraphicalGeneratorBuilder<BddGraphicalGenerator, Integer>, BddGraphicalGenerator> constructor) {
            super(constructor);
            defaultEdgeStyle = DEFAULT_TRUE_EDGE_STYLE;
            nodeStyleMapper = (index) -> {
                if (index == BDD_FALSE) {
                    return defaultFalseNodeStyle;
                } else if (index == BDD_TRUE) {
                    return defaultTrueNodeStyle;
                } else {
                    return defaultNodeStyle;
                }
            };
        }

        /**
         * Sets the default style for the TRUE terminal node of the BDD. This
         * style will be ignored when a dynamic node style mapper is configured.
         * @param trueNodeStyle the node style
         * @return the current builder
         */
        public BddGraphicalGeneratorBuilder trueNodeStyle(final GraphicalNodeStyle trueNodeStyle) {
            defaultTrueNodeStyle = trueNodeStyle;
            return this;
        }

        /**
         * Sets the default style for the FALSE terminal node of the BDD. This
         * style will be ignored when a dynamic node style mapper is configured.
         * @param falseNodeStyle the node style
         * @return the current builder
         */
        public BddGraphicalGeneratorBuilder falseNodeStyle(final GraphicalNodeStyle falseNodeStyle) {
            defaultFalseNodeStyle = falseNodeStyle;
            return this;
        }

        /**
         * Sets the default edge style for negative (low) edges in the BDD.
         * @param negativeEdgeStyle the edge style
         * @return the current builder
         */
        public BddGraphicalGeneratorBuilder negativeEdgeStyle(final GraphicalEdgeStyle negativeEdgeStyle) {
            this.negativeEdgeStyle = negativeEdgeStyle;
            return this;
        }

        /**
         * Sets the negative edge mapper for dynamically computing edge styles
         * for negative edges in the BDD. If this mapper is configured, the
         * default edge style for negative edges is ignored and each edge is
         * styled by the computed style of
         * {@link EdgeStyleMapper#computeStyle(Object, Object)}.
         * @param negativeEdgeMapper the edge mapper
         * @return the current builder
         */
        public BddGraphicalGeneratorBuilder negativeEdgeMapper(final EdgeStyleMapper<Integer> negativeEdgeMapper) {
            this.negativeEdgeMapper = negativeEdgeMapper;
            return this;
        }
    }
}
