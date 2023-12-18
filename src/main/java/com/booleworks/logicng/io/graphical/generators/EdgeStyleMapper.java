// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;

/**
 * A style mapper for generating graphical representations of formulas, BDDs and
 * graphs. This mapper can be used to compute an edge style for the given edge
 * between two node contents.
 * @param <T> the type of the node content
 * @version 2.4.0
 * @since 2.4.0
 */
public interface EdgeStyleMapper<T> {

    /**
     * Computes a style for the given edge between two node contents.
     * @param source      the content of the source node
     * @param destination the content of the destination node
     * @return the style for the node with this content
     */
    GraphicalEdgeStyle computeStyle(T source, T destination);
}
