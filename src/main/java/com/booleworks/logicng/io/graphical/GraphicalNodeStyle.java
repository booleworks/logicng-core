// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical;

import java.util.Objects;

/**
 * The style of a node in a graphical representation of a formula, BDD, or
 * graph. The style consists of the shape, and the stroke, text, and background
 * color.
 * @version 2.4.0
 * @since 2.4.0
 */
public class GraphicalNodeStyle {

    /**
     * A special node style which does not set any style, so the framework's
     * (dot/Mermaid.js) default theming is applied.
     */
    private static final GraphicalNodeStyle NO_NODE_STYLE = new GraphicalNodeStyle(null, null, null, null);

    /**
     * The shape of the node.
     */
    public enum Shape {
        RECTANGLE,
        ELLIPSE,
        CIRCLE
    }

    private final Shape shape;
    private final GraphicalColor strokeColor;
    private final GraphicalColor textColor;
    private final GraphicalColor backgroundColor;

    /**
     * Private constructor. Use factory methods to construct an instance.
     * @param shape           the shape of the node
     * @param strokeColor     the color for the node strokes
     * @param textColor       the color for the text of the node
     * @param backgroundColor the color for the background of the node
     */
    private GraphicalNodeStyle(final Shape shape, final GraphicalColor strokeColor, final GraphicalColor textColor,
                               final GraphicalColor backgroundColor) {
        this.shape = shape;
        this.strokeColor = strokeColor;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    /**
     * Returns a special node style which does not set any style, so the
     * framework's (dot/Mermaid.js) default theming is applied.
     * @return special node style which does not set any style
     */
    public static GraphicalNodeStyle noStyle() {
        return NO_NODE_STYLE;
    }

    /**
     * Constructs a new node style with the given values.
     * @param shape           the shape of the node
     * @param strokeColor     the color for the node strokes
     * @param textColor       the color for the text of the node
     * @param backgroundColor the color for the background of the node
     * @return the new node style
     */
    public static GraphicalNodeStyle style(final Shape shape, final GraphicalColor strokeColor,
                                           final GraphicalColor textColor,
                                           final GraphicalColor backgroundColor) {
        if (shape == null && strokeColor == null && textColor == null && backgroundColor == null) {
            return NO_NODE_STYLE;
        } else {
            return new GraphicalNodeStyle(shape, strokeColor, textColor, backgroundColor);
        }
    }

    /**
     * Constructs a new circle node style with the given values.
     * @param strokeColor     the color for the node strokes
     * @param textColor       the color for the text of the node
     * @param backgroundColor the color for the background of the node
     * @return the new node style
     */
    public static GraphicalNodeStyle circle(final GraphicalColor strokeColor, final GraphicalColor textColor,
                                            final GraphicalColor backgroundColor) {
        return new GraphicalNodeStyle(Shape.CIRCLE, strokeColor, textColor, backgroundColor);
    }

    /**
     * Constructs a new ellipse node style with the given values.
     * @param strokeColor     the color for the node strokes
     * @param textColor       the color for the text of the node
     * @param backgroundColor the color for the background of the node
     * @return the new node style
     */
    public static GraphicalNodeStyle ellipse(final GraphicalColor strokeColor, final GraphicalColor textColor,
                                             final GraphicalColor backgroundColor) {
        return new GraphicalNodeStyle(Shape.ELLIPSE, strokeColor, textColor, backgroundColor);
    }

    /**
     * Constructs a new rectangle node style with the given values.
     * @param strokeColor     the color for the node strokes
     * @param textColor       the color for the text of the node
     * @param backgroundColor the color for the background of the node
     * @return the new node style
     */
    public static GraphicalNodeStyle rectangle(final GraphicalColor strokeColor, final GraphicalColor textColor,
                                               final GraphicalColor backgroundColor) {
        return new GraphicalNodeStyle(Shape.RECTANGLE, strokeColor, textColor, backgroundColor);
    }

    /**
     * Returns whether this style has any value set. If not it is equivalent to
     * {@link GraphicalNodeStyle#NO_NODE_STYLE}.
     * @return whether this style has any value set
     */
    public boolean hasStyle() {
        return !equals(NO_NODE_STYLE);
    }

    /**
     * Returns the shape of this node style.
     * @return the shape of this node style
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * Returns the stroke color of this node style.
     * @return the stroke color of this node style
     */
    public GraphicalColor getStrokeColor() {
        return strokeColor;
    }

    /**
     * Returns the text color of this node style.
     * @return the text color of this node style
     */
    public GraphicalColor getTextColor() {
        return textColor;
    }

    /**
     * Returns the background color of this node style.
     * @return the background color of this node style
     */
    public GraphicalColor getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GraphicalNodeStyle that = (GraphicalNodeStyle) o;
        return shape == that.shape &&
                Objects.equals(strokeColor, that.strokeColor) &&
                Objects.equals(textColor, that.textColor) &&
                Objects.equals(backgroundColor, that.backgroundColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape, strokeColor, textColor, backgroundColor);
    }

    @Override
    public String toString() {
        return "GraphicalNodeStyle{" +
                "shape=" + shape +
                ", strokeColor='" + strokeColor + '\'' +
                ", textColor='" + textColor + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                '}';
    }
}
