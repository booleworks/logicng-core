// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical;

import java.util.Objects;

/**
 * A single node in a graphical representation of a formula, BDD, or graph. A
 * node holds a unique ID, a label, a flag whether this is a terminal node or
 * not, and a node style.
 * @version 2.4.0
 * @since 2.4.0
 */
public class GraphicalNode {
    private final String id;
    private final String label;
    private final boolean terminal;
    private final GraphicalNodeStyle style;

    /**
     * Constructs a new non-terminal graphical node with the given values.
     * @param id    the unique ID
     * @param label the label
     * @param style the style of the node
     */
    public GraphicalNode(final String id, final String label, final GraphicalNodeStyle style) {
        this(id, label, false, style);
    }

    /**
     * Constructs a new graphical node with the given values.
     * @param id       the unique ID
     * @param label    the label
     * @param terminal whether it is a terminal node or not
     * @param style    the style of the node
     */
    public GraphicalNode(final String id, final String label, final boolean terminal, final GraphicalNodeStyle style) {
        this.id = id;
        this.label = label;
        this.terminal = terminal;
        this.style = style;
    }

    /**
     * Returns the id of this node.
     * @return the if of this node
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the label of this node.
     * @return the label of this node
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns whether this node is a terminal.
     * @return {@code true} if this node is a terminal, otherwise {@code false}
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Returns the style of this node.
     * @return the style of this node
     */
    public GraphicalNodeStyle getStyle() {
        return style;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GraphicalNode that = (GraphicalNode) o;
        return terminal == that.terminal &&
                Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, terminal, style);
    }

    @Override
    public String toString() {
        return "GraphicalNode{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", terminal=" + terminal +
                ", style=" + style +
                '}';
    }
}
