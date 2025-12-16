// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import java.util.Iterator;

/**
 * A state storing a certain point in the iteration over the elements of an
 * SDD decomposition node.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddNodeIterationState {
    protected final SddNodeDecomposition node;
    protected Iterator<SddElement> iterator;
    protected SddElement current;
    protected int generation = 0;

    /**
     * Constructs a new iteration state for an SDD decomposition node.
     * @param node the SDD decomposition node
     */
    public SddNodeIterationState(final SddNodeDecomposition node) {
        this.node = node;
        reset();
    }

    /**
     * Reset the state to the first element.
     */
    public void reset() {
        this.iterator = node.iterator();
        this.current = next();
    }

    /**
     * Advances to the next element of the node.
     */
    public SddElement next() {
        if (!iterator.hasNext()) {
            current = null;
            return null;
        }
        current = iterator.next();
        if (current.getSub().isFalse()) {
            current = null;
            return next();
        }
        return current;
    }

    /**
     * Returns the current element of this state.
     * @return the current element of this state
     */
    public SddElement getActiveElement() {
        return current;
    }

    /**
     * Returns the generation of this state.
     * @return the generation of this state
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * Updates the generation of this state.
     * @param generation the new generation
     */
    public void setGeneration(final int generation) {
        this.generation = generation;
    }
}
